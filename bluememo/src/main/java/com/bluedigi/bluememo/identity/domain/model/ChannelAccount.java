package com.bluedigi.bluememo.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.bluedigi.bluememo.messaging.domain.ChannelType;

import lombok.Data;

@Data
public class ChannelAccount {
    private UUID id;
    private UUID userId;
    private ChannelType channelType;
    private String externalUserId;
    private String externalChatId;
    private Instant linkedAt;
}
