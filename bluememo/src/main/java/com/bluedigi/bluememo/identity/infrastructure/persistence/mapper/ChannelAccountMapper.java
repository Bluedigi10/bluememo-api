package com.bluedigi.bluememo.identity.infrastructure.persistence.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.bluedigi.bluememo.identity.domain.model.ChannelAccount;
import com.bluedigi.bluememo.identity.domain.model.ChannelLinkToken;
import com.bluedigi.bluememo.identity.infrastructure.persistence.entity.ChannelAccountEntity;
import com.bluedigi.bluememo.identity.infrastructure.persistence.entity.ChannelLinkTokenEntity;
import com.bluedigi.bluememo.identity.infrastructure.persistence.entity.UserEntity;
import com.bluedigi.bluememo.identity.infrastructure.web.request.CreateChannelLinkToken;

@Component
public class ChannelAccountMapper {

    public ChannelLinkToken toDomain(CreateChannelLinkToken channel) {
        ChannelLinkToken challenge = new ChannelLinkToken();
        challenge.setUserId(UUID.fromString(channel.userId()));
        challenge.setChannelType(channel.channelType());
        return challenge;
    }

    public ChannelLinkTokenEntity toEntity(ChannelLinkToken domain, UserEntity user) {
        ChannelLinkTokenEntity entity = new ChannelLinkTokenEntity();
        entity.setId(domain.getId());
        entity.setUser(user);
        entity.setChannelType(domain.getChannelType());
        entity.setTokenHash(domain.getTokenHash());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setConsentedAt(domain.getConsentedAt());
        entity.setConsentVersion(domain.getConsentVersion());
        return entity;
    }

    public ChannelLinkToken toDomain(ChannelLinkTokenEntity entity) {
        ChannelLinkToken domain = new ChannelLinkToken();
        domain.setId(entity.getId());
        domain.setUserId(entity.getUser().getId());
        domain.setChannelType(entity.getChannelType());
        domain.setTokenHash(entity.getTokenHash());
        domain.setExpiresAt(entity.getExpiresAt());
        domain.setUsedAt(entity.getUsedAt() != null ? entity.getUsedAt() : null);
        domain.setConsentedAt(entity.getConsentedAt());
        domain.setConsentVersion(entity.getConsentVersion());
        return domain;
    }

    public ChannelAccountEntity toEntity(ChannelAccount domain, UserEntity user) {
        ChannelAccountEntity entity = new ChannelAccountEntity();
        entity.setId(domain.getId());
        entity.setUser(user);
        entity.setChannelType(domain.getChannelType());
        entity.setExternalUserId(domain.getExternalUserId());
        entity.setExternalChatId(domain.getExternalChatId());
        entity.setConsentedAt(domain.getConsentedAt());
        entity.setConsentVersion(domain.getConsentVersion());
        return entity;
    }

}
