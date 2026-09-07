package com.bluedigi.bluememo.identity.domain.repository;

import java.util.UUID;

import com.bluedigi.bluememo.identity.domain.model.ChannelAccount;

public interface ChannelAccountRepository {
    void saveChannelLinkAccount(ChannelAccount account);
    Boolean existsByUserIdAndChannelType(UUID userId, String channelType);
}
