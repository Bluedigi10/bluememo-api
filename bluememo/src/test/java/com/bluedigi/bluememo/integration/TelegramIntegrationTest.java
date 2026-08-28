package com.bluedigi.bluememo.integration;

import com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.dto.TelegramHeader;
import com.bluedigi.bluememo.messaging.domain.MessageConstants;
import com.bluedigi.bluememo.messaging.infrastructure.persistence.repository.IncomingEventJpaRepository;
import com.bluedigi.bluememo.testsupport.IntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@AutoConfigureMockMvc
class TelegramIntegrationTest {
    private static final String TELEGRAM_WH_SECRET = "WH_SECRET";
    private static final String TELEGRAM_BOT_TOKEN = "BOT_TOKEN";
    private static final String TEXT_MESSAGE = "Hello World!";
    private static final String EMPTY = "";
    private static final String C_START = "/start";
    private static final String C_HELP = "/help";
    private static final String C_MUSIC = "/music";
    
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IncomingEventJpaRepository incomingEventJpaRepository;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    private static final MockWebServer server = new MockWebServer();

    @BeforeAll
    static void setUp() throws IOException {
        server.start();
    }

    @BeforeEach
    void cleanDatabase() {
        incomingEventJpaRepository.deleteAll();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "bluememo.telegram.webhook-secret",
                () -> TELEGRAM_WH_SECRET
        );
        registry.add(
                "bluememo.telegram.bot-token",
                () -> TELEGRAM_BOT_TOKEN
        );
        registry.add(
                "bluememo.telegram.api-base-url",
                () -> server.url("/bot").toString()
        );
    }

    @AfterAll
    static void tearDown() {
        server.close();
    }

    @Test
    void sendMessageCompleteFlowSuccess() throws Exception {
        String updateBody = updateBody(root ->  {
            root.put("update_id", 10000);
            message(root).put("text", TEXT_MESSAGE);
        });
        server.enqueue(
                new MockResponse.Builder()
                        .code(200)
                        .addHeader("Content-Type", "application/json")
                        .body(successMessageSendBody(TEXT_MESSAGE))
                        .build()
        );
        mockMvc.perform(post("/webhooks/telegram")
                        .header(TelegramHeader.TELEGRAM_HEADER, TELEGRAM_WH_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getUrl().encodedPath()).isEqualTo("/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage");
        assertThat(request.getBody()).isNotNull();
        assertThat(request.getBody().utf8()).contains("Recibí " + TEXT_MESSAGE);
    }

    @Test
    void sendMessageCompleteFlowWithIndepomtencySuccess() throws Exception {
        String updateBody = updateBody(root ->  {
            root.put("update_id", 10000);
            message(root).put("text", TEXT_MESSAGE);
        });
        server.enqueue(
                new MockResponse.Builder()
                        .code(200)
                        .addHeader("Content-Type", "application/json")
                        .body(successMessageSendBody(TEXT_MESSAGE))
                        .build()
        );
        mockMvc.perform(post("/webhooks/telegram")
                        .header(TelegramHeader.TELEGRAM_HEADER, TELEGRAM_WH_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getUrl().encodedPath()).isEqualTo("/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage");
        assertThat(request.getBody()).isNotNull();
        assertThat(request.getBody().utf8()).contains("Recibí " + TEXT_MESSAGE);

        String updateBody2 = updateBody(root ->  {
            root.put("update_id", 10000);
            message(root).put("text", TEXT_MESSAGE);
        });
        mockMvc.perform(post("/webhooks/telegram")
                        .header(TelegramHeader.TELEGRAM_HEADER, TELEGRAM_WH_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody2))
                .andExpect(status().isOk());

        RecordedRequest requestNull = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(requestNull).isNull();
    }

    @Test
    void telegramWebHookSecretError()  throws Exception {
        String updateBody = updateBody(root -> message(root).put("text", TEXT_MESSAGE));
        mockMvc.perform(post("/webhooks/telegram")
                        .header(TelegramHeader.TELEGRAM_HEADER, "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void messageNullFlow()  throws Exception {
        String updateNullMessageBody = updateBody(root -> root.remove("message"));
        mockMvc.perform(post("/webhooks/telegram")
                        .header(TelegramHeader.TELEGRAM_HEADER, TELEGRAM_WH_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateNullMessageBody))
                .andExpect(status().isOk());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

        assertThat(request).isNull();
    }

    @Test
    void messageTextNullFlow()  throws Exception {
        String updateTextNullBody = updateBody(root -> message(root).remove("text"));
        server.enqueue(
                new MockResponse.Builder()
                        .code(200)
                        .addHeader("Content-Type", "application/json")
                        .body(successMessageSendBody(TEXT_MESSAGE))
                        .build()
        );
        mockMvc.perform(post("/webhooks/telegram")
                        .header(TelegramHeader.TELEGRAM_HEADER, TELEGRAM_WH_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateTextNullBody))
                .andExpect(status().isOk());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

        assertThat(request).isNotNull();
        assertThat(request.getUrl().encodedPath()).isEqualTo("/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody()).isNotNull();
        String body = request.getBody().utf8();
        assertThat(body)
            .isNotNull()
            .contains("De momento solo proceso texto");
    }

    @Test
    void messageTextEmptyFlow()  throws Exception {
        String updateBody = updateBody(root -> message(root).put("text", EMPTY));
        server.enqueue(
                new MockResponse.Builder()
                        .code(200)
                        .addHeader("Content-Type", "application/json")
                        .body(successMessageSendBody("De momento solo proceso texto"))
                        .build()
        );
        mockMvc.perform(post("/webhooks/telegram")
                        .header(TelegramHeader.TELEGRAM_HEADER, TELEGRAM_WH_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

        assertThat(request).isNotNull();
        assertThat(request.getUrl().encodedPath()).isEqualTo("/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody()).isNotNull();
        String body = request.getBody().utf8();
        assertThat(body)
            .isNotNull()
            .contains("De momento solo proceso texto");
    }

    @Test
    void messageCommandStartFlow()  throws Exception {
        String updateBody = updateBody(root -> message(root).put("text", C_START));
        server.enqueue(
                new MockResponse.Builder()
                        .code(200)
                        .addHeader("Content-Type", "application/json")
                        .body(successMessageSendBody(MessageConstants.START))
                        .build()
        );
        mockMvc.perform(post("/webhooks/telegram")
                        .header(TelegramHeader.TELEGRAM_HEADER, TELEGRAM_WH_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

        assertThat(request).isNotNull();
        assertThat(request.getUrl().encodedPath()).isEqualTo("/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody()).isNotNull();
        String body = request.getBody().utf8();
        assertThat(body)
            .isNotNull()
            .contains(MessageConstants.START);
    }

    @Test
    void messageCommandHelpFlow()  throws Exception {
        String updateBody = updateBody(root -> message(root).put("text", C_HELP));
        server.enqueue(
                new MockResponse.Builder()
                        .code(200)
                        .addHeader("Content-Type", "application/json")
                        .body(successMessageSendBody(MessageConstants.HELP))
                        .build()
        );
        mockMvc.perform(post("/webhooks/telegram")
                        .header(TelegramHeader.TELEGRAM_HEADER, TELEGRAM_WH_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

        assertThat(request).isNotNull();
        assertThat(request.getUrl().encodedPath()).isEqualTo("/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody()).isNotNull();
        String body = request.getBody().utf8();
        assertThat(body)
            .isNotNull()
            .contains(MessageConstants.HELP);
    }

    @Test
    void messageCommandUnknownFlow()  throws Exception {
        String updateBody = updateBody(root -> message(root).put("text", C_MUSIC));
        server.enqueue(
                new MockResponse.Builder()
                        .code(200)
                        .addHeader("Content-Type", "application/json")
                        .body(successMessageSendBody(MessageConstants.DEFAULT))
                        .build()
        );
        mockMvc.perform(post("/webhooks/telegram")
                        .header(TelegramHeader.TELEGRAM_HEADER, TELEGRAM_WH_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

        assertThat(request).isNotNull();
        assertThat(request.getUrl().encodedPath()).isEqualTo("/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody()).isNotNull();
        String body = request.getBody().utf8();
        assertThat(body)
            .isNotNull()
            .contains(MessageConstants.DEFAULT);
    }

    @Test
    void sendMessageError500Response() throws Exception {
        String updateBody = updateBody(root -> message(root).put("text", TEXT_MESSAGE));
        server.enqueue(
                new MockResponse.Builder()
                        .code(500)
                        .addHeader("Content-Type", "application/json")
                        .build()
        );
        mockMvc.perform(post("/webhooks/telegram")
                        .header(TelegramHeader.TELEGRAM_HEADER, TELEGRAM_WH_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isInternalServerError());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getUrl().encodedPath()).isEqualTo("/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage");
    }

    @Test
    void sendMessageBodyResponseNull() throws Exception {
        String updateBody = updateBody(root -> message(root).put("text", TEXT_MESSAGE));
        server.enqueue(
                new MockResponse.Builder()
                        .code(200)
                        .addHeader("Content-Type", "application/json")
                        .build()
        );
        mockMvc.perform(post("/webhooks/telegram")
                        .header(TelegramHeader.TELEGRAM_HEADER, TELEGRAM_WH_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isInternalServerError());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getUrl().encodedPath()).isEqualTo("/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage");
    }

    @Test
    void shouldIgnoreMessageWithoutChat() throws Exception {
        String updateWithoutChatBody = updateBody(root -> message(root).remove("chat"));
        mockMvc.perform(post("/webhooks/telegram")
                        .header(
                                TelegramHeader.TELEGRAM_HEADER,
                                TELEGRAM_WH_SECRET
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateWithoutChatBody))
                .andExpect(status().isOk());

        RecordedRequest outboundRequest =
                server.takeRequest(200, TimeUnit.MILLISECONDS);

        assertThat(outboundRequest).isNull();
    }

    @Test
    void shouldIgnoreMessageWithoutFrom() throws Exception {
        String updateWithoutFromBody = updateBody(root -> message(root).remove("from"));
        mockMvc.perform(post("/webhooks/telegram")
                        .header(
                                TelegramHeader.TELEGRAM_HEADER,
                                TELEGRAM_WH_SECRET
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateWithoutFromBody))
                .andExpect(status().isOk());

        RecordedRequest outboundRequest =
                server.takeRequest(200, TimeUnit.MILLISECONDS);

        assertThat(outboundRequest).isNull();
    }

    @Test
    void shouldIgnoreMessageWithoutDate() throws Exception {
        String updateWithoutDateBody = updateBody(root -> message(root).remove("date"));
        mockMvc.perform(post("/webhooks/telegram")
                        .header(
                                TelegramHeader.TELEGRAM_HEADER,
                                TELEGRAM_WH_SECRET
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateWithoutDateBody))
                .andExpect(status().isOk());

        RecordedRequest outboundRequest =
                server.takeRequest(200, TimeUnit.MILLISECONDS);

        assertThat(outboundRequest).isNull();
    }

    @Test
    void shouldIgnoreMessageWithoutMessageId() throws Exception {
        String updateWithoutMessageIdBody = updateBody(root -> message(root).remove("message_id"));
        mockMvc.perform(post("/webhooks/telegram")
                        .header(
                                TelegramHeader.TELEGRAM_HEADER,
                                TELEGRAM_WH_SECRET
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateWithoutMessageIdBody))
                .andExpect(status().isOk());

        RecordedRequest outboundRequest =
                server.takeRequest(200, TimeUnit.MILLISECONDS);

        assertThat(outboundRequest).isNull();
    }

    @Test
    void shouldIgnoreMessageWithoutUpdateId() throws Exception {
        String updateWithoutUpdateIdBody = updateBody(root -> root.remove("update_id"));
        mockMvc.perform(post("/webhooks/telegram")
                        .header(
                                TelegramHeader.TELEGRAM_HEADER,
                                TELEGRAM_WH_SECRET
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateWithoutUpdateIdBody))
                .andExpect(status().isOk());

        RecordedRequest outboundRequest =
                server.takeRequest(200, TimeUnit.MILLISECONDS);

        assertThat(outboundRequest).isNull();
    }

    @Test
    void shouldIgnoreMessageWithoutChatId() throws Exception {
        String updateChatIdNullBody = updateBody(root -> chat(root).remove("id"));
        mockMvc.perform(post("/webhooks/telegram")
                        .header(
                                TelegramHeader.TELEGRAM_HEADER,
                                TELEGRAM_WH_SECRET
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateChatIdNullBody))
                .andExpect(status().isOk());

        RecordedRequest outboundRequest =
                server.takeRequest(200, TimeUnit.MILLISECONDS);

        assertThat(outboundRequest).isNull();
    }

    @Test
    void shouldIgnoreMessageWithoutFromId() throws Exception {
        String updateFromIdNullBody = updateBody(root -> from(root).remove("id"));
        mockMvc.perform(post("/webhooks/telegram")
                        .header(
                                TelegramHeader.TELEGRAM_HEADER,
                                TELEGRAM_WH_SECRET
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateFromIdNullBody))
                .andExpect(status().isOk());

        RecordedRequest outboundRequest =
                server.takeRequest(200, TimeUnit.MILLISECONDS);

        assertThat(outboundRequest).isNull();
    }

    private ObjectNode validUpdateBody() {
        ObjectMapper mapper = new ObjectMapper();
        
        ObjectNode root = mapper.createObjectNode();
        
        root.put("update_id", 10000);
        
        ObjectNode message = root.putObject("message");
        message.put("message_id", 123);
        message.put("date", 1234);
        message.put("text", "Hola");
        
        ObjectNode from = message.putObject("from");
        from.put("id", 124);
        from.put("is_bot", true);
        from.put("first_name", "Emma");
        from.put("username", "emmatest");
        
        ObjectNode chat = message.putObject("chat");
        chat.put("id", 1235);
        chat.put("type", "private");
        
        return root;
    }

    private String updateBody(Consumer<ObjectNode> modifier) {
        ObjectNode root = validUpdateBody();
        modifier.accept(root);
        return root.toString();
    }
    
    private ObjectNode message(ObjectNode root) {
        return (ObjectNode) root.path("message");
    }
    
    private ObjectNode from(ObjectNode root) {
        return (ObjectNode) root.path("message").path("from");
    }
    
    private ObjectNode chat(ObjectNode root) {
        return (ObjectNode) root.path("message").path("chat");
    }

    private String successMessageSendBody(String text){
        return """
                {
                  "ok": true,
                  "result": {
                    "message_id": 321,
                    "date": 1234,
                    "text": "%s",
                    "chat": {
                      "id": 1235,
                      "type": "private"
                    }
                  }
                }
                """.formatted(text);
    }

}
