package com.bluedigi.bluememo.identity.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import com.bluedigi.bluememo.identity.application.port.ChannelIdentityResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bluedigi.bluememo.common.domain.ChannelType;
import com.bluedigi.bluememo.common.exception.BluememoException;
import com.bluedigi.bluememo.common.exception.StatusCodeError;
import com.bluedigi.bluememo.identity.config.LinkProperties;
import com.bluedigi.bluememo.identity.domain.model.ChannelAccount;
import com.bluedigi.bluememo.identity.domain.model.ChannelLinkToken;
import com.bluedigi.bluememo.identity.domain.repository.ChannelAccountRepository;
import com.bluedigi.bluememo.identity.domain.repository.ChannelLinkTokenRepository;
import com.bluedigi.bluememo.identity.infrastructure.persistence.mapper.ChannelAccountMapper;
import com.bluedigi.bluememo.identity.infrastructure.web.request.CreateChannelLinkToken;
import com.bluedigi.bluememo.identity.infrastructure.web.response.LinkChannelResponse;

import lombok.AllArgsConstructor;
import java.util.List;

@AllArgsConstructor
@Service
public class ChannelAccountService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    public static final String CONSENT_VERSION = "1";
    private final ChannelLinkTokenRepository channelLinkTokenRepository;
    private final ChannelAccountRepository channelAccountRepository;
    private final ChannelAccountMapper channelAccountMapper;
    private final ChannelIdentityResolver channelIdentityResolver;
    private final LinkProperties linkProperties;

    @Transactional(readOnly = true)
    public List<ChannelAccount> getLinks(String userId, ChannelType channelType) {
        return channelAccountRepository.findByUserId(UUID.fromString(userId), channelType);
    }

    @Transactional
    public void unlinkChannel(String userId, ChannelType channelType) {
        UUID userUUID = UUID.fromString(userId);
        requireUserLock(userUUID);
        channelLinkTokenRepository.invalidate(userUUID, channelType);
        channelAccountRepository.revoke(userUUID, channelType);
    }

    @Transactional(readOnly = true)
    public boolean isLinked(ChannelType channelType, String externalUserId, String externalConversationId) {
        return channelIdentityResolver.resolve(channelType, externalUserId, externalConversationId).isPresent();
    }

    @Transactional
    public LinkChannelResponse generateLink(CreateChannelLinkToken request) {
        if (!request.consent()) {
            throw new BluememoException("Se requiere consentimiento para vincular el canal", StatusCodeError.BAD_REQUEST.getStatusCode());
        }
        requireUserLock(UUID.fromString(request.userId()));
        boolean linkAccountExists = channelAccountRepository.existsByUserIdAndChannelType(UUID.fromString(request.userId()), request.channelType());

        if (linkAccountExists) {
            throw new BluememoException("Ya tienes una cuenta vinculada a este canal", StatusCodeError.CONFLICT.getStatusCode());
        }
        Duration expirationDuration = Duration.ofMinutes(10);

        String token = generateToken();
        Instant expirationDate = Instant.now().plus(expirationDuration);

        ChannelLinkToken channelLinkToken = channelAccountMapper.toDomain(request);
        channelLinkToken.setTokenHash(hashToken(token));
        channelLinkToken.setExpiresAt(expirationDate);
        channelLinkToken.setConsentedAt(Instant.now());
        channelLinkToken.setConsentVersion(CONSENT_VERSION);

        String linkUrl = generateLinkUrl(request.channelType(), token);

        try {
            channelLinkTokenRepository.saveChannelLinkToken(channelLinkToken);
        } catch (RuntimeException e) {
            throw new BluememoException("Failed to save channel link token", StatusCodeError.INTERNAL_SERVER_ERROR.getStatusCode(), e);
        }

        return new LinkChannelResponse(linkUrl, expirationDate);
    }

    @Transactional
    public String linkAccount(String externalUserId, ChannelType messageChannelType, String externalChatId, String token) {

        String tokenHash = hashToken(token);

        Optional<ChannelLinkToken> tokenFound = channelLinkTokenRepository.findByTokenHash(tokenHash);

        if (tokenFound.isEmpty()) {
            return "Token inválido";
        }

        ChannelLinkToken existingToken = tokenFound.get();

        if (!existingToken.isValidAt(Instant.now())) {
            return "Token inválido";
        }

        UUID userId = existingToken.getUserId();

        // Serialize generation, linking, unlinking and deletion for this BlueMemo user.
        if (!channelAccountRepository.lockUser(userId)) {
            return "Token inválido";
        }
        if (existingToken.getConsentedAt() == null || existingToken.getConsentVersion() == null) {
            return "Token inválido";
        }

        ChannelType channelType = existingToken.getChannelType();

        if (existingToken.getChannelType() != messageChannelType) {
            return "El token no coincide con el canal";
        }

        ChannelAccount account = new ChannelAccount();
        account.setUserId(userId);
        account.setChannelType(channelType);
        account.setExternalUserId(externalUserId);
        account.setExternalChatId(externalChatId);
        account.setConsentedAt(existingToken.getConsentedAt());
        account.setConsentVersion(existingToken.getConsentVersion());

        boolean isMarkedAsUsed = channelLinkTokenRepository.markTokenAsUsed(tokenHash);

        if (!isMarkedAsUsed) {
            return "Token inválido o ya usado";
        }

        boolean linkAccountExists = channelAccountRepository.existsByUserIdAndChannelType(userId, channelType);
        boolean externalAccountExists = channelAccountRepository.existsByExternalUserIdAndChannelType(externalUserId, channelType);

        if (linkAccountExists) {
            return "Ya tienes una cuenta vinculada a este canal";
        }

        if (externalAccountExists) {
            return "Este canal ya está vinculado a otra cuenta";
        }

        // A concurrent ownership conflict must not abort the transaction and undo consumption.
        if (!channelAccountRepository.insertIfAvailable(account)) {
            return "El usuario o la conversación ya tiene una vinculación activa";
        }
        return "Cuenta vinculada con éxito. ¡Bienvenido!";
    }

    private void requireUserLock(UUID userId) {
        if (!channelAccountRepository.lockUser(userId)) {
            throw new BluememoException("User not found", 404);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String generateLinkUrl(ChannelType channelType, String token) {
        String baseUrl;
        if (ChannelType.TELEGRAM == channelType) {
            baseUrl = linkProperties.telegramUrl() + linkProperties.telegramBotName() + "?start=";
        } else {
            throw new BluememoException("Unsupported channel type: " + channelType, StatusCodeError.BAD_REQUEST.getStatusCode());
        }
        return baseUrl + token;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
