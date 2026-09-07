package com.bluedigi.bluememo.identity.infrastructure.persistence;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.bluedigi.bluememo.common.exception.BluememoException;
import com.bluedigi.bluememo.common.exception.StatusCodeError;
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
    public void saveChannelLinkToken(ChannelLinkToken token) {
        UUID userId = token.getUserId();
        UserEntity user = userJpaRepository.getReferenceById(userId);
        ChannelLinkTokenEntity entity = mapper.toEntity(token, user);
        entity.setId(UUID.randomUUID());
        jpaRepository.upsert(entity);
    }

    @Override
    public Boolean markTokenAsUsed(String tokenHash) {
        int rowsUpdated =jpaRepository.markTokenAsUsed(tokenHash);
        return rowsUpdated == 1;
    }

    @Override
    public ChannelLinkToken findByTokenHash(String tokenHash) {
        ChannelLinkTokenEntity entity = jpaRepository.findByTokenHash(tokenHash)
        .orElseThrow(() -> new BluememoException("Token inválido", StatusCodeError.BAD_REQUEST.getStatusCode()));

        return mapper.toDomain(entity);
    }
}
