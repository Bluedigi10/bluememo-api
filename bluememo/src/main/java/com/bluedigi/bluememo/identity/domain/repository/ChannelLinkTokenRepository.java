package com.bluedigi.bluememo.identity.domain.repository;

import com.bluedigi.bluememo.identity.domain.model.ChannelLinkToken;

public interface ChannelLinkTokenRepository {
    void saveChannelLinkToken(ChannelLinkToken token);
    Boolean markTokenAsUsed(String tokenHash);
    ChannelLinkToken findByTokenHash(String tokenHash);
}
