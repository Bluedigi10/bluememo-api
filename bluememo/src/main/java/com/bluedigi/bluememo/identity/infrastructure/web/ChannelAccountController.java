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
import com.bluedigi.bluememo.identity.domain.model.ChannelAccount;
import com.bluedigi.bluememo.identity.infrastructure.web.request.ChannelLinkConsentRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@AllArgsConstructor
@RequestMapping("/users/me")
public class ChannelAccountController {
    private final ChannelAccountService channelAccountService;

    @GetMapping("/link/channel/{channelType}")
    public List<ChannelAccount> getLinks(
            @PathVariable ChannelType channelType, @AuthenticationPrincipal UserDetails loggedUser) {
        return channelAccountService.getLinks(loggedUser.getUsername(), channelType);
    }

    @PostMapping ("/link/channel/{channelType}")
    public ResponseEntity<LinkChannelResponse> generateChannelLink(@PathVariable ChannelType channelType, @AuthenticationPrincipal UserDetails loggedUser,
            @Valid @RequestBody ChannelLinkConsentRequest consent) {
        CreateChannelLinkToken request = new CreateChannelLinkToken(loggedUser.getUsername(), channelType, Boolean.TRUE.equals(consent.consent()));
        LinkChannelResponse response = channelAccountService.generateLink(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping ("/unlink/channel/{channelType}")
    public ResponseEntity<Void> unlinkChannel(@PathVariable ChannelType channelType, @AuthenticationPrincipal UserDetails loggedUser) {
        channelAccountService.unlinkChannel(loggedUser.getUsername(), channelType);
        return ResponseEntity.noContent().build();
    }
}
