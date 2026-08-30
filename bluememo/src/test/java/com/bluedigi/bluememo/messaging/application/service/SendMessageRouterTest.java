package com.bluedigi.bluememo.messaging.application.service;

import com.bluedigi.bluememo.messaging.application.exception.MessageChannelNotConfiguredException;
import com.bluedigi.bluememo.messaging.application.port.out.ChannelMessageSender;
import com.bluedigi.bluememo.messaging.domain.OutgoingMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.bluedigi.bluememo.messaging.domain.ChannelType.TELEGRAM;
import static com.bluedigi.bluememo.messaging.domain.ChannelType.WHATSAPP;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendMessageRouterTest {
    private static final String EXTERNAL_MESSAGE_ID = "EXTERNAL MESSAGE ID";
    @Mock
    private ChannelMessageSender telegramSender;

    @Mock
    private ChannelMessageSender whatsappSender;

    private SendMessageRouter router;

    @BeforeEach
    void setUp() {
        when(telegramSender.supportedChannelType())
                .thenReturn(TELEGRAM);

        when(whatsappSender.supportedChannelType())
                .thenReturn(WHATSAPP);

        router = new SendMessageRouter(
                List.of(telegramSender, whatsappSender)
        );
    }

    @Test
    void shouldRouteMessageToWhatsappSender() {
        OutgoingMessage message = new OutgoingMessage(
                WHATSAPP,
                EXTERNAL_MESSAGE_ID,
                "whatsapp-conversation-id",
                "Hola desde WhatsApp"
        );

        router.send(message);

        verify(whatsappSender).send(message);
        verify(telegramSender, never()).send(any());
    }

    @Test
    void shouldRouteMessageToTelegramSender() {
        OutgoingMessage message = new OutgoingMessage(
                TELEGRAM,
                EXTERNAL_MESSAGE_ID,
                "telegram-chat-id",
                "Hola desde Telegram"
        );

        router.send(message);

        verify(telegramSender).send(message);
        verify(whatsappSender, never()).send(any());
    }

    @Test
    void shouldThrowExceptionWhenChannelIsNotConfigured() {
        SendMessageRouter routerWithoutWhatsapp =
                new SendMessageRouter(List.of(telegramSender));

        OutgoingMessage message = new OutgoingMessage(
                WHATSAPP,
                EXTERNAL_MESSAGE_ID,
                "whatsapp-conversation-id",
                "Hola"
        );

        assertThatThrownBy(() -> routerWithoutWhatsapp.send(message))
                .isInstanceOf(MessageChannelNotConfiguredException.class)
                .hasMessageContaining("WHATSAPP");

        verify(telegramSender, never()).send(any());
    }
}
