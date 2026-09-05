package com.bluedigi.bluememo.identity.application.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.springframework.stereotype.Service;

import com.bluedigi.bluememo.common.exception.BluememoException;
import com.bluedigi.bluememo.common.exception.StatusCodeError;
import com.bluedigi.bluememo.identity.domain.model.ChannelLinkToken;
import com.bluedigi.bluememo.identity.domain.repository.ChannelLinkTokenRepository;
import com.bluedigi.bluememo.identity.infrastructure.persistence.mapper.ChannelAccountMapper;
import com.bluedigi.bluememo.identity.infrastructure.web.request.CreateChannelLinkToken;
import com.bluedigi.bluememo.identity.infrastructure.web.response.LinkChannelResponse;

@Service
public class ChannelAccountService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final ChannelLinkTokenRepository channelLinkTokenRepository;
    private final ChannelAccountMapper channelAccountMapper;

    public ChannelAccountService(ChannelLinkTokenRepository channelLinkTokenRepository) {
        this.channelLinkTokenRepository = channelLinkTokenRepository;
        this.channelAccountMapper = new ChannelAccountMapper();
    }

    public LinkChannelResponse generateLink(CreateChannelLinkToken request) {

        Duration expirationDuration = Duration.ofMinutes(15);
        String token = generateToken();
        String linkUrl = request.channelType() + token;
        Instant expirationDate = Instant.now().plus(expirationDuration);

        ChannelLinkToken channelLinkToken = channelAccountMapper.toDomain(request);
        channelLinkToken.setTokenHash(hashToken(token));
        channelLinkToken.setExpiresAt(expirationDate);

        try {
            channelLinkTokenRepository.saveChannelLinkToken(channelLinkToken);
        } catch (RuntimeException e) {
            throw new BluememoException("Failed to save channel link token", StatusCodeError.INTERNAL_SERVER_ERROR.getStatusCode(), e.getCause());
        }

        return new LinkChannelResponse(linkUrl, expirationDate);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hashToken(String token) {
        return Base64.getEncoder().encodeToString(token.getBytes());
    }
}
