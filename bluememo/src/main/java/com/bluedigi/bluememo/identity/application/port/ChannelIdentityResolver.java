package com.bluedigi.bluememo.identity.application.port;

import com.bluedigi.bluememo.common.domain.ChannelType;
import java.util.Optional;
import java.util.UUID;

public interface ChannelIdentityResolver {
    Optional<UUID> resolve(ChannelType channelType, String externalUserId, String externalConversationId);
}
