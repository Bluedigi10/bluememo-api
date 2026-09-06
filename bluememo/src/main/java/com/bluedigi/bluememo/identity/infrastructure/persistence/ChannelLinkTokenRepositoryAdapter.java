package com.bluedigi.bluememo.identity.infrastructure.persistence;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.bluedigi.bluememo.identity.domain.model.ChannelLinkToken;
import com.bluedigi.bluememo.identity.domain.repository.ChannelLinkTokenRepository;
import com.bluedigi.bluememo.identity.infrastructure.persistence.entity.ChannelLinkTokenEntity;
import com.bluedigi.bluememo.identity.infrastructure.persistence.entity.UserEntity;
import com.bluedigi.bluememo.identity.infrastructure.persistence.mapper.ChannelAccountMapper;
import com.bluedigi.bluememo.identity.infrastructure.persistence.repository.ChannelLinkTokenJpaRepository;
import com.bluedigi.bluememo.identity.infrastructure.persistence.repository.UserJpaRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Repository
class ChannelLinkTokenRepositoryAdapter implements ChannelLinkTokenRepository {

    private final ChannelLinkTokenJpaRepository jpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final ChannelAccountMapper mapper;

    @Override
    public Boolean saveChannelLinkToken(ChannelLinkToken token) {
        UUID userId = token.getUserId();
        UserEntity user = userJpaRepository.getReferenceById(userId);
        ChannelLinkTokenEntity entity = mapper.toEntity(token, user);
        entity.setId(UUID.randomUUID());
        int rowsAffected = jpaRepository.upsert(entity);
        return rowsAffected == 1;
    }

    @Override
    public Boolean markTokenAsUsed(String tokenHash) {
        int rowsUpdated =jpaRepository.markTokenAsUsed(tokenHash);
        return rowsUpdated == 1;
    }


}
