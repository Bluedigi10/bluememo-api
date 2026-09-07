package com.bluedigi.bluememo.identity.infrastructure.persistence;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.bluedigi.bluememo.common.domain.ChannelType;
import com.bluedigi.bluememo.identity.domain.model.ChannelAccount;
import com.bluedigi.bluememo.identity.domain.repository.ChannelAccountRepository;
import com.bluedigi.bluememo.identity.infrastructure.persistence.entity.ChannelAccountEntity;
import com.bluedigi.bluememo.identity.infrastructure.persistence.entity.UserEntity;
import com.bluedigi.bluememo.identity.infrastructure.persistence.mapper.ChannelAccountMapper;
import com.bluedigi.bluememo.identity.infrastructure.persistence.repository.ChannelAccountJpaRepository;
import com.bluedigi.bluememo.identity.infrastructure.persistence.repository.UserJpaRepository;

import lombok.AllArgsConstructor;

@Repository
@AllArgsConstructor
public class ChannelAccountRepositoryAdapter implements ChannelAccountRepository {
    private final ChannelAccountJpaRepository channelAccountJpaRepository;
    private final ChannelAccountMapper channelAccountMapper;
    private final UserJpaRepository userJpaRepository;

    @Override
    public void saveChannelLinkAccount(ChannelAccount account) {
        UserEntity userEntity = getUserEntityFromChannelAccount(account);
        ChannelAccountEntity entity = channelAccountMapper.toEntity(account, userEntity);
        channelAccountJpaRepository.save(entity);
    }

    @Override
    public boolean existsByUserIdAndChannelType(UUID userId, ChannelType channelType) {
        return channelAccountJpaRepository.existsByUser_IdAndChannelType(userId, channelType.name());
    }

    private UserEntity getUserEntityFromChannelAccount(ChannelAccount account) {
        UUID userId = account.getUserId();
        return userJpaRepository.getReferenceById(userId);
    }

}
