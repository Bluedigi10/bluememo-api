package com.bluedigi.bluememo.channel.telegram.inbound.infrastructure.webhook;

import com.bluedigi.bluememo.channel.telegram.config.TelegramProperties;
import com.bluedigi.bluememo.channel.telegram.inbound.infrastructure.webhook.mapper.TelegramUpdateMapper;
import com.bluedigi.bluememo.messaging.domain.IncomingMessage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bluedigi.bluememo.channel.telegram.inbound.infrastructure.webhook.request.TelegramUpdateRequest;
import com.bluedigi.bluememo.messaging.application.port.in.ProcessIncomingMessageUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Objects;


@RestController
@RequestMapping("/webhooks/telegram")
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final ProcessIncomingMessageUseCase processIncomingMessageUseCase;
    private final TelegramUpdateMapper telegramUpdateMapper;
    private final TelegramProperties properties;
    
    @PostMapping
    public ResponseEntity<Void> receiveWebhook(
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String receivedSecret,
            @RequestBody TelegramUpdateRequest entity
    ) {
        if (!Objects.equals(properties.webhookSecret(), receivedSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        IncomingMessage message = telegramUpdateMapper.mapToIncomingMessage(entity);
        processIncomingMessageUseCase.process(message);

        return ResponseEntity.ok().build();
    }
    
}
