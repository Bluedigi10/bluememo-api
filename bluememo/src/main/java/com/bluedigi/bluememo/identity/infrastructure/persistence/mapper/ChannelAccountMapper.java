package com.bluedigi.bluememo.identity.infrastructure.persistence.mapper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.bluedigi.bluememo.identity.domain.model.ChannelLinkToken;
import com.bluedigi.bluememo.identity.infrastructure.persistence.entity.ChannelLinkTokenEntity;
import com.bluedigi.bluememo.identity.infrastructure.persistence.entity.UserEntity;
import com.bluedigi.bluememo.identity.infrastructure.web.request.CreateChannelLinkToken;
import com.bluedigi.bluememo.messaging.domain.ChannelType;

@Component
public class ChannelAccountMapper {

    public ChannelLinkToken toDomain(CreateChannelLinkToken channel) {
        ChannelLinkToken challenge = new ChannelLinkToken();
        challenge.setUserId(UUID.fromString(channel.userId()));
        challenge.setChannelType(ChannelType.fromValue(channel.channelType()));
        return challenge;
    }

    public ChannelLinkTokenEntity toEntity(ChannelLinkToken domain, UserEntity user) {
        ChannelLinkTokenEntity entity = new ChannelLinkTokenEntity();
        entity.setId(domain.getId());
        entity.setUser(user);
        entity.setChannelType(domain.getChannelType());
        entity.setTokenHash(domain.getTokenHash());
        entity.setExpiresAt(LocalDateTime.ofInstant(domain.getExpiresAt(), ZoneId.systemDefault()));
        return entity;
    }

}
