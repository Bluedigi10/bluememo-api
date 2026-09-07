package com.bluedigi.bluememo.identity.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bluedigi.bluememo.identity.infrastructure.persistence.entity.ChannelAccountEntity;

public interface ChannelAccountJpaRepository extends JpaRepository<ChannelAccountEntity, UUID> {
    Boolean existsByUserIdAndChannelType(UUID userId, String channelType);
}
