package com.bluedigi.bluememo.identity.infrastructure.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bluedigi.bluememo.common.domain.ChannelType;
import com.bluedigi.bluememo.identity.application.service.ChannelAccountService;
import com.bluedigi.bluememo.identity.infrastructure.web.request.CreateChannelLinkToken;
import com.bluedigi.bluememo.identity.infrastructure.web.response.LinkChannelResponse;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/users/me")
public class ChannelAccountController {
    private final ChannelAccountService channelAccountService;

    @PostMapping ("/link/channel/{channelType}")
    public ResponseEntity<LinkChannelResponse> generateChannelLink(@PathVariable ChannelType channelType, @AuthenticationPrincipal UserDetails loggedUser) {
        CreateChannelLinkToken request = new CreateChannelLinkToken(loggedUser.getUsername(), channelType);
        LinkChannelResponse response = channelAccountService.generateLink(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping ("/unlink/channel/{channelType}")
    public ResponseEntity<Void> unlinkChannel(@PathVariable ChannelType channelType, @AuthenticationPrincipal UserDetails loggedUser) {
        channelAccountService.unlinkChannel(loggedUser.getUsername(), channelType);
        return ResponseEntity.noContent().build();
    }
}
