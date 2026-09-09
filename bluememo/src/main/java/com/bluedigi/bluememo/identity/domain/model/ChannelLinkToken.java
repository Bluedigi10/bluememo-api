package com.bluedigi.bluememo.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.bluedigi.bluememo.common.domain.ChannelType;

import lombok.Data;

@Data
public class ChannelLinkToken {
    private UUID id;
    private UUID userId;
    private ChannelType channelType;
    private String tokenHash;
    private Instant expiresAt;
    private Instant usedAt;
    private Instant consentedAt;
    private String consentVersion;

    public boolean isValidAt(Instant now) {
        return usedAt == null && expiresAt.isAfter(now);
    }
}
