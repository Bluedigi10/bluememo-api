package com.bluedigi.bluememo.identity.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.bluedigi.bluememo.identity.domain.model.ChannelLinkToken;

public interface ChannelLinkTokenRepository {
    void saveChannelLinkToken(ChannelLinkToken token);
    Boolean markTokenAsUsed(String tokenHash);
    Optional<ChannelLinkToken> findByTokenHash(String tokenHash);
    void deleteByUserId(UUID userId);
}
