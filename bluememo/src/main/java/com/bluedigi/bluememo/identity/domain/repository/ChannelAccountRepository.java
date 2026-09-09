package com.bluedigi.bluememo.identity.domain.repository;

import java.util.UUID;

import com.bluedigi.bluememo.common.domain.ChannelType;
import com.bluedigi.bluememo.identity.domain.model.ChannelAccount;

public interface ChannelAccountRepository {
    void saveChannelLinkAccount(ChannelAccount account);
    boolean existsByUserIdAndChannelType(UUID userId, ChannelType channelType);
    boolean existsByExternalUserIdAndChannelType(String externalUserId, ChannelType channelType);
    void deleteByUserIdAndChannelType(UUID userId, ChannelType channelType);
    void deleteByUserId(UUID userId);
}
