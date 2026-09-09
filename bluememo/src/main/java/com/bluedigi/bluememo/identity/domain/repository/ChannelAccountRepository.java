package com.bluedigi.bluememo.identity.domain.repository;

import java.util.UUID;

import com.bluedigi.bluememo.common.domain.ChannelType;
import com.bluedigi.bluememo.identity.domain.model.ChannelAccount;
import java.util.List;

public interface ChannelAccountRepository {
    boolean insertIfAvailable(ChannelAccount account);
    boolean lockUser(UUID userId);
    boolean existsByUserIdAndChannelType(UUID userId, ChannelType channelType);
    boolean existsByExternalUserIdAndChannelType(String externalUserId, ChannelType channelType);
    void revoke(UUID userId, ChannelType channelType);
    void deleteByUserId(UUID userId);
    List<ChannelAccount> findByUserId(UUID userId, ChannelType channelType);
}
