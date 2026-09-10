package com.bluedigi.bluememo.channel.telegram.utils;

import com.bluedigi.bluememo.messaging.domain.enums.IncomingAction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;

class TelegramActionConverterTest {
    private final TelegramActionConverter converter = new TelegramActionConverter();

    @ParameterizedTest
    @MethodSource("messages")
    void convertsChannelSyntaxWithoutLosingMessageOrToken(String text, IncomingAction action, String payload) {
        var result = converter.parseAction(text);
        assertThat(result.action()).isEqualTo(action);
        assertThat(result.message()).isEqualTo(payload);
    }

    static Stream<Arguments> messages() {
        return Stream.of(
                Arguments.of(null, IncomingAction.MESSAGE, null),
                Arguments.of("", IncomingAction.MESSAGE, ""),
                Arguments.of("Hola 👋 /start", IncomingAction.MESSAGE, "Hola 👋 /start"),
                Arguments.of("/start", IncomingAction.WELCOME, "/start"),
                Arguments.of("/start   ", IncomingAction.WELCOME, "/start   "),
                Arguments.of("/start abc-_123", IncomingAction.LINK_CHANNEL, "abc-_123"),
                Arguments.of("/start\t  abc-_123  ", IncomingAction.LINK_CHANNEL, "abc-_123"),
                Arguments.of("/start first second", IncomingAction.LINK_CHANNEL, "first second"),
                Arguments.of("/check-link", IncomingAction.CHECK_CHANNEL_LINK, "/check-link"),
                Arguments.of("/help", IncomingAction.HELP, "/help"),
                Arguments.of("/unrecognized", IncomingAction.UNKNOWN_COMMAND, "/unrecognized")
        );
    }
}
