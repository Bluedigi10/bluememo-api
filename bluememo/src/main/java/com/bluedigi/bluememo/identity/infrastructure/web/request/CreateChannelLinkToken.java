package com.bluedigi.bluememo.identity.infrastructure.web.request;

import com.bluedigi.bluememo.common.domain.ChannelType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateChannelLinkToken(
    @NotBlank
    String userId,
    @NotNull
    ChannelType channelType,
    boolean consent
) {
}
