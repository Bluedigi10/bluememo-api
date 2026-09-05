package com.bluedigi.bluememo.identity.domain.repository;

import com.bluedigi.bluememo.identity.domain.model.ChannelLinkToken;

public interface ChannelLinkTokenRepository {
    void saveChannelLinkToken(ChannelLinkToken token);
    void markTokenAsUsed(String tokenHash);
}
