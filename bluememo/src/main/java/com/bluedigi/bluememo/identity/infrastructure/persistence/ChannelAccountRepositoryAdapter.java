package com.bluedigi.bluememo.identity.infrastructure.persistence;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.bluedigi.bluememo.common.domain.ChannelType;
import com.bluedigi.bluememo.identity.domain.model.ChannelAccount;
import com.bluedigi.bluememo.identity.domain.repository.ChannelAccountRepository;
import com.bluedigi.bluememo.identity.infrastructure.persistence.repository.ChannelAccountJpaRepository;

import lombok.AllArgsConstructor;
import com.bluedigi.bluememo.identity.application.port.ChannelIdentityResolver;
import java.util.List;
import java.util.Optional;

@Repository
@AllArgsConstructor
public class ChannelAccountRepositoryAdapter implements ChannelAccountRepository, ChannelIdentityResolver {
    private final ChannelAccountJpaRepository channelAccountJpaRepository;

    @Override
    public boolean insertIfAvailable(ChannelAccount account) {
        return channelAccountJpaRepository.insertIfAvailable(UUID.randomUUID(), account) == 1;
    }

    @Override
    public boolean existsByUserIdAndChannelType(UUID userId, ChannelType channelType) {
        return channelAccountJpaRepository.existsByUser_IdAndChannelTypeAndRevokedAtIsNull(userId, channelType);
    }

    @Override
    public boolean lockUser(UUID userId) {
        return channelAccountJpaRepository.lockUser(userId).isPresent();
    }

    @Override
    public boolean existsByExternalUserIdAndChannelType(String externalUserId, ChannelType channelType) {
        return channelAccountJpaRepository.existsByExternalUserIdAndChannelTypeAndRevokedAtIsNull(externalUserId, channelType);
    }

    @Override
    public void revoke(UUID userId, ChannelType channelType) {
        channelAccountJpaRepository.revoke(userId, channelType.name());
    }

    @Override
    public void deleteByUserId(UUID userId) {
        channelAccountJpaRepository.deleteByUser_Id(userId);
    }

    @Override
    public Optional<UUID> resolve(ChannelType channelType, String externalUserId, String externalConversationId) {
        return channelAccountJpaRepository.resolve(channelType, externalUserId, externalConversationId);
    }

    @Override
    public List<ChannelAccount> findByUserId(UUID userId, ChannelType channelType) {
        return channelAccountJpaRepository.findByUser_IdAndChannelTypeOrderByLinkedAtDesc(userId, channelType).stream()
                .map(entity -> {
                    ChannelAccount account = new ChannelAccount();
                    account.setId(entity.getId());
                    account.setUserId(userId);
                    account.setChannelType(entity.getChannelType());
                    account.setExternalUserId(entity.getExternalUserId());
                    account.setExternalChatId(entity.getExternalChatId());
                    account.setLinkedAt(entity.getLinkedAt());
                    account.setRevokedAt(entity.getRevokedAt());
                    account.setConsentedAt(entity.getConsentedAt());
                    account.setConsentVersion(entity.getConsentVersion());
                    return account;
                }).toList();
    }

}
