package com.bluedigi.bluememo.identity.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bluedigi.bluememo.identity.infrastructure.persistence.entity.ChannelLinkTokenEntity;

public interface ChannelLinkTokenJpaRepository extends JpaRepository<ChannelLinkTokenEntity, UUID> {
    ChannelLinkTokenEntity findByTokenHash(String tokenHash);
    @Modifying
    @Query(value = """
            INSERT INTO channel_link_tokens (
                id,
                user_id,
                channel_type,
                token_hash,
                expires_at,
                used_at
            )
            VALUES (
                :#{#token.id},
                :#{#token.user.id},
                :#{#token.channelType.name()},
                :#{#token.tokenHash},
                :#{#token.expiresAt},
                NULL
            )
            ON CONFLICT ON CONSTRAINT uk_channel_type_user_channel
            DO UPDATE SET
                token_hash = EXCLUDED.token_hash,
                expires_at = EXCLUDED.expires_at,
                used_at = NULL
            """, nativeQuery = true)
    int insertIfAbsent(
        @Param("token") ChannelLinkTokenEntity token
    );

    @Modifying
    @Query (value = """
            UPDATE channel_link_tokens
            SET used_at = CURRENT_TIMESTAMP
            WHERE token_hash = :tokenHash
            """, nativeQuery = true)
    int markTokenAsUsed(@Param("tokenHash") String tokenHash);
}
