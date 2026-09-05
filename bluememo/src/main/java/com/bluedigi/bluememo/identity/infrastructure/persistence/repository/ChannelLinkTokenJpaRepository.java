package com.bluedigi.bluememo.identity.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bluedigi.bluememo.identity.infrastructure.persistence.entity.ChannelLinkTokenEntity;

public interface ChannelLinkTokenJpaRepository extends JpaRepository<ChannelLinkTokenEntity, UUID> {
    ChannelLinkTokenEntity findByTokenHash(String tokenHash);
}
