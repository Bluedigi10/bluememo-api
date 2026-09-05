package com.bluedigi.bluememo.identity.infrastructure.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bluedigi.bluememo.identity.application.service.ChannelAccountService;
import com.bluedigi.bluememo.identity.infrastructure.web.request.CreateChannelLinkToken;
import com.bluedigi.bluememo.identity.infrastructure.web.response.LinkChannelResponse;
import com.bluedigi.bluememo.messaging.domain.ChannelType;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/users")
public class ChannelAccountController {
    private final ChannelAccountService channelAccountService;

    @PostMapping ("me/link/channel/{channelType}")
    public ResponseEntity<LinkChannelResponse> putMethodName(@PathVariable ChannelType channelType, @AuthenticationPrincipal UserDetails loggedUser) {
        CreateChannelLinkToken request = new CreateChannelLinkToken(loggedUser.getUsername(), channelType.name());
        LinkChannelResponse response = channelAccountService.generateLink(request);
        return ResponseEntity.ok(response);
    }
}
