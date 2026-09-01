package com.bluedigi.bluememo.channel.telegram.inbound.infrastructure.webhook;

import com.bluedigi.bluememo.channel.telegram.config.TelegramProperties;
import com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.dto.TelegramHeader;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bluedigi.bluememo.channel.telegram.inbound.infrastructure.webhook.request.TelegramUpdateRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Objects;


@RestController
@RequestMapping("/webhooks/telegram")
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final TelegramProperties properties;
    private final TelegramUpdateProcess telegramUpdateProcess;

    @PostMapping
    public ResponseEntity<Void> receiveWebhook(
            @RequestHeader(value = TelegramHeader.TELEGRAM_HEADER, required = false) String receivedSecret,
            @RequestBody TelegramUpdateRequest request
    ) {
        if (!Objects.equals(properties.webhookSecret(), receivedSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        telegramUpdateProcess.process(request);

        return ResponseEntity.ok().build();
    }

}
