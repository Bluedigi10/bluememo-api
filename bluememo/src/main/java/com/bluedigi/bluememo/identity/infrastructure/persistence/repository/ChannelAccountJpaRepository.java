package com.bluedigi.bluememo.identity.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bluedigi.bluememo.common.domain.ChannelType;
import com.bluedigi.bluememo.identity.infrastructure.persistence.entity.ChannelAccountEntity;

public interface ChannelAccountJpaRepository extends JpaRepository<ChannelAccountEntity, UUID> {
    boolean existsByUser_IdAndChannelType(UUID userId, ChannelType channelType);
    boolean existsByExternalUserIdAndChannelType(String externalUserId, ChannelType channelType);
    void deleteByUser_IdAndChannelType(UUID userId, ChannelType channelType);
    void deleteByUser_Id(UUID userId);
}
