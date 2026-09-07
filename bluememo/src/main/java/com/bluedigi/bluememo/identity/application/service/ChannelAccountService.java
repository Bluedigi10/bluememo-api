package com.bluedigi.bluememo.identity.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

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

@AllArgsConstructor
@Service
public class ChannelAccountService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final ChannelLinkTokenRepository channelLinkTokenRepository;
    private final ChannelAccountRepository channelAccountRepository;
    private final ChannelAccountMapper channelAccountMapper;
    private final LinkProperties linkProperties;

    @Transactional
    public LinkChannelResponse generateLink(CreateChannelLinkToken request) {
        Duration expirationDuration = Duration.ofMinutes(15);

        String token = generateToken();
        Instant expirationDate = Instant.now().plus(expirationDuration);

        ChannelLinkToken channelLinkToken = channelAccountMapper.toDomain(request);
        channelLinkToken.setTokenHash(hashToken(token));
        channelLinkToken.setExpiresAt(expirationDate);

        String linkUrl = generateLinkUrl(request.channelType(), token);

        try {
            channelLinkTokenRepository.saveChannelLinkToken(channelLinkToken);
        } catch (RuntimeException e) {
            throw new BluememoException("Failed to save channel link token", StatusCodeError.INTERNAL_SERVER_ERROR.getStatusCode(), e);
        }

        return new LinkChannelResponse(linkUrl, expirationDate);
    }

    @Transactional
    public String linkAccount(ChannelAccount account, String token) {

        String tokenHash = hashToken(token);

        ChannelLinkToken existingToken = channelLinkTokenRepository.findByTokenHash(tokenHash);

        if (existingToken == null || existingToken.getUsedAt() != null) {
            return "Token inválido o ya usado";
        }

        if (existingToken.getExpiresAt().isBefore(Instant.now())) {
            return "Token expirado";
        }

        Boolean accountExists = channelAccountRepository.existsByUserIdAndChannelType(account.getUserId(), account.getChannelType().name());

        if (Boolean.TRUE.equals(accountExists)) {
            return "Ya tienes una cuenta vinculada con este canal";
        }

        channelAccountRepository.saveChannelLinkAccount(account);

        Boolean isMarkedAsUsed = channelLinkTokenRepository.markTokenAsUsed(tokenHash);

        if (Boolean.TRUE.equals(isMarkedAsUsed)) {
            return "Cuenta vinculada con éxito. ¡Bienvenido!";
        } else {
            return "Token inválido o ya usado";
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
