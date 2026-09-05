package com.bluedigi.bluememo.identity.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;

public record CreateChannelLinkToken(
    @NotBlank
    String userId,
    @NotBlank
    String channelType
) {
}
