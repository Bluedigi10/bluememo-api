package com.bluedigi.bluememo.channel.telegram.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TelegramMessageSplitterTest {

    private static final int MAX_LENGTH = 4096;
    private static final String EMOJI = "😀";

    private TelegramMessageSplitter splitter;

    @BeforeEach
    void setUp() {
        splitter = new TelegramMessageSplitter();
    }

    @Test
    void shouldReturnSingleMessageWhenTextDoesNotExceedLimit() {
        String text = "a".repeat(MAX_LENGTH - 1);

        List<String> messages = splitter.split(text);

        assertThat(messages)
                .containsExactly(text);
    }

    @Test
    void shouldReturnSingleMessageWhenTextHasExactlyMaxCodePoints() {
        String text = "a".repeat(MAX_LENGTH);

        List<String> messages = splitter.split(text);

        assertThat(messages)
                .containsExactly(text);
    }

    @Test
    void shouldSplitMessageWithoutLosingContent() {
        String text = "word ".repeat(1000) + "end";

        List<String> messages = splitter.split(text);

        assertThat(messages).hasSize(2);
        assertChunksWithinLimit(messages);
        assertThat(String.join("", messages))
                .isEqualTo(text);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\n\n",
            "\n",
            " "
    })
    void shouldPreserveSeparatorInFirstMessageWhenUsedAsSplitPoint(
            String separator
    ) {
        String text = "a".repeat(4090)
                + separator
                + "b".repeat(100);

        List<String> messages = splitter.split(text);

        assertThat(messages).hasSize(2);

        assertThat(messages.get(0))
                .endsWith(separator);

        assertChunksWithinLimit(messages);

        assertThat(String.join("", messages))
                .isEqualTo(text);
    }

    @Test
    void shouldHardSplitAtMaxLengthWhenThereIsNoSeparator() {
        String text = "a".repeat(MAX_LENGTH + 1);

        List<String> messages = splitter.split(text);

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0)).hasSize(MAX_LENGTH);
        assertThat(messages.get(1)).isEqualTo("a");

        assertThat(String.join("", messages))
                .isEqualTo(text);
    }

    @Test
    void shouldCountEmojiAsSingleCodePointAtExactLimit() {
        String text = EMOJI.repeat(MAX_LENGTH);

        List<String> messages = splitter.split(text);

        assertThat(messages)
                .containsExactly(text);

        assertThat(
                messages.get(0)
                        .codePointCount(0, messages.get(0).length())
        ).isEqualTo(MAX_LENGTH);
    }

    @Test
    void shouldSplitEmojiWithoutBreakingSurrogatePairs() {
        String text = EMOJI.repeat(MAX_LENGTH + 1);

        List<String> messages = splitter.split(text);

        assertThat(messages).hasSize(2);

        assertThat(
                messages.get(0)
                        .codePointCount(0, messages.get(0).length())
        ).isEqualTo(MAX_LENGTH);

        assertThat(messages.get(1))
                .isEqualTo(EMOJI);

        assertThat(messages)
                .allSatisfy(message ->
                        assertThat(
                                StandardCharsets.UTF_8
                                        .newEncoder()
                                        .canEncode(message)
                        ).isTrue()
                );

        assertThat(String.join("", messages))
                .isEqualTo(text);
    }

    private void assertChunksWithinLimit(List<String> messages) {
        assertThat(messages).isNotNull();
        assertThat(messages).allSatisfy(message ->
                        assertThat(
                                message.codePointCount(
                                        0,
                                        message.length()
                                )
                        ).isLessThanOrEqualTo(MAX_LENGTH)
                );
    }
}
