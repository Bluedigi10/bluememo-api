package com.bluedigi.bluememo.identity.infrastructure.persistence.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;

import com.bluedigi.bluememo.messaging.domain.ChannelType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "channel_link_tokens")
public class ChannelLinkTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChannelType channelType;
    @Column(nullable = false)
    private String tokenHash;
    @Column (nullable = false)
    private LocalDateTime expiresAt;
    @UpdateTimestamp
    private LocalDateTime usedAt;
    private Boolean isUsed;
}
