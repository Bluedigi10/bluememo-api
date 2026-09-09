package com.bluedigi.bluememo.identity.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bluedigi.bluememo.identity.infrastructure.persistence.entity.ChannelLinkTokenEntity;

public interface ChannelLinkTokenJpaRepository extends JpaRepository<ChannelLinkTokenEntity, UUID> {
    Optional<ChannelLinkTokenEntity> findByTokenHash(String tokenHash);
    @Modifying
    @Query(value = """
            INSERT INTO channel_link_tokens (
                id,
                user_id,
                channel_type,
                token_hash,
                expires_at,
                used_at,
                consented_at,
                consent_version
            )
            VALUES (
                :#{#token.id},
                :#{#token.user.id},
                :#{#token.channelType.name()},
                :#{#token.tokenHash},
                :#{#token.expiresAt},
                NULL,
                :#{#token.consentedAt},
                :#{#token.consentVersion}
            )
            ON CONFLICT ON CONSTRAINT uk_channel_type_user_channel
            DO UPDATE SET
                token_hash = EXCLUDED.token_hash,
                expires_at = EXCLUDED.expires_at,
                used_at = NULL,
                consented_at = EXCLUDED.consented_at,
                consent_version = EXCLUDED.consent_version
            """, nativeQuery = true)
    int upsert(
        @Param("token") ChannelLinkTokenEntity token
    );

    @Modifying
    @Query (value = """
            UPDATE channel_link_tokens
            SET used_at = CURRENT_TIMESTAMP
            WHERE token_hash = :tokenHash
            AND used_at IS NULL
            AND expires_at > CURRENT_TIMESTAMP
            """, nativeQuery = true)
    int markTokenAsUsed(@Param("tokenHash") String tokenHash);
    void deleteByUser_Id(UUID userId);

    @Modifying
    @Query(value = "UPDATE channel_link_tokens SET used_at = CURRENT_TIMESTAMP WHERE user_id = :userId AND channel_type = :channel AND used_at IS NULL", nativeQuery = true)
    int invalidate(@Param("userId") UUID userId, @Param("channel") String channel);
}
