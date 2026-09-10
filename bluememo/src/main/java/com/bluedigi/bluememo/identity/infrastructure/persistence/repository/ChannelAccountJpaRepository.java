package com.bluedigi.bluememo.identity.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bluedigi.bluememo.common.domain.ChannelType;
import com.bluedigi.bluememo.identity.infrastructure.persistence.entity.ChannelAccountEntity;
import com.bluedigi.bluememo.identity.domain.model.ChannelAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChannelAccountJpaRepository extends JpaRepository<ChannelAccountEntity, UUID> {
    boolean existsByUser_IdAndChannelTypeAndRevokedAtIsNull(UUID userId, ChannelType channelType);
    boolean existsByExternalUserIdAndChannelTypeAndRevokedAtIsNull(String externalUserId, ChannelType channelType);
    void deleteByUser_Id(UUID userId);
    List<ChannelAccountEntity> findByUser_IdAndChannelTypeOrderByLinkedAtDesc(UUID userId, ChannelType channelType);

    @Query("SELECT a.user.id FROM ChannelAccountEntity a WHERE a.channelType = :channel AND a.externalUserId = :externalUser AND a.externalChatId = :chat AND a.revokedAt IS NULL")
    Optional<UUID> resolve(@Param("channel") ChannelType channel,
            @Param("externalUser") String externalUser,
            @Param("chat") String chat);

    @Query(value = "SELECT id FROM users WHERE id = :userId FOR UPDATE", nativeQuery = true)
    Optional<UUID> lockUser(@Param("userId") UUID userId);

    @Modifying
    @Query(value = "UPDATE channel_accounts SET revoked_at = CURRENT_TIMESTAMP WHERE user_id = :userId AND channel_type = :channel AND revoked_at IS NULL", nativeQuery = true)
    int revoke(@Param("userId") UUID userId,
               @Param("channel") String channel);

    @Modifying
    @Query(value = """
            INSERT INTO channel_accounts
                (id, user_id, channel_type, external_user_id, external_chat_id, linked_at, consented_at, consent_version)
            VALUES (:id, :#{#account.userId}, :#{#account.channelType.name()},
                :#{#account.externalUserId}, :#{#account.externalChatId}, CURRENT_TIMESTAMP,
                :#{#account.consentedAt}, :#{#account.consentVersion})
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIfAvailable(@Param("id") UUID id,
            @Param("account") ChannelAccount account);
}
