package com.bluedigi.bluememo.identity.infrastructure.persistence;

import java.time.Instant;
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
    public void saveChannelLinkToken(ChannelLinkToken token) {
        UUID userId = token.getUserId();
        UserEntity user = userJpaRepository.getReferenceById(userId);
        ChannelLinkTokenEntity entity = mapper.toEntity(token, user);
        jpaRepository.insertIfAbsent(entity);
    }

    @Override
    public void markTokenAsUsed(String tokenHash) {
        ChannelLinkTokenEntity entity = jpaRepository.findByTokenHash(tokenHash);
        if (entity == null) {
            throw new IllegalArgumentException("Invalid token");
        }

        entity.setUsedAt(Instant.now());
            jpaRepository.save(entity);
    }


}
