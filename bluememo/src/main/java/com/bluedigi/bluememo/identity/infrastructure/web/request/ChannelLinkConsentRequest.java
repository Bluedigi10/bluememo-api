package com.bluedigi.bluememo.identity.infrastructure.web.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record ChannelLinkConsentRequest(
        @NotNull @AssertTrue Boolean consent
) {
}
