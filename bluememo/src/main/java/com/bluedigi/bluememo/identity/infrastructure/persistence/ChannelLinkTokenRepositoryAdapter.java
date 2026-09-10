package com.bluedigi.bluememo.identity.infrastructure.persistence;

import java.util.Optional;
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
import com.bluedigi.bluememo.common.domain.ChannelType;

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
    public Optional<ChannelLinkToken> findByTokenHash(String tokenHash) {
        Optional<ChannelLinkTokenEntity> entity = jpaRepository.findByTokenHash(tokenHash);

        return entity.map(mapper::toDomain);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        jpaRepository.deleteByUser_Id(userId);
    }

    @Override
    public void invalidate(UUID userId, ChannelType channelType) {
        jpaRepository.invalidate(userId, channelType.name());
    }
}
