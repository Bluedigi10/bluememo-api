package com.bluedigi.bluememo.identity.infrastructure.web.request;

import com.bluedigi.bluememo.common.domain.ChannelType;

import jakarta.validation.constraints.NotBlank;

public record CreateChannelLinkToken(
    @NotBlank
    String userId,
    @NotBlank
    ChannelType channelType
) {
}
