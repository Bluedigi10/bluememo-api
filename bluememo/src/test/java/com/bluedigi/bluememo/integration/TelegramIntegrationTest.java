package com.bluedigi.bluememo.integration;

import com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.dto.TelegramHeader;
import com.bluedigi.bluememo.messaging.domain.MessageConstants;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TelegramIntegrationTest {
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
    private static final MockWebServer server = new MockWebServer();

    static {
        try {
            server.start();
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
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
                        .content(updateBody(TEXT_MESSAGE)))
                .andExpect(status().isOk());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getUrl().encodedPath()).isEqualTo("/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage");
    }

    @Test
    void telegramWebHookSecretError()  throws Exception {
        mockMvc.perform(post("/webhooks/telegram")
                        .header(TelegramHeader.TELEGRAM_HEADER, "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(TEXT_MESSAGE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void messageNullFlow()  throws Exception {
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
                        .content(updateNullMessageBody()))
                .andExpect(status().isOk());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

        assertThat(request).isNull();
    }

    @Test
    void messageTextNullFlow()  throws Exception {
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
                        .content(updateTextNullBody()))
                .andExpect(status().isOk());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

        assertThat(request).isNotNull();
        assertThat(request.getUrl().encodedPath()).isEqualTo("/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody()).isNotNull();
        String body = request.getBody().utf8();
        assertThat(body).isNotNull();
        assertThat(body).contains("De momento solo proceso texto");
    }

    @Test
    void messageTextEmptyFlow()  throws Exception {
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
                        .content(updateBody(EMPTY)))
                .andExpect(status().isOk());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

        assertThat(request).isNotNull();
        assertThat(request.getUrl().encodedPath()).isEqualTo("/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody()).isNotNull();
        String body = request.getBody().utf8();
        assertThat(body).isNotNull();
        assertThat(body).contains("De momento solo proceso texto");
    }

    @Test
    void messageCommandStartFlow()  throws Exception {
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
                        .content(updateBody(C_START)))
                .andExpect(status().isOk());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

        assertThat(request).isNotNull();
        assertThat(request.getUrl().encodedPath()).isEqualTo("/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody()).isNotNull();
        String body = request.getBody().utf8();
        assertThat(body).isNotNull();
        assertThat(body).contains(MessageConstants.START);
    }

    @Test
    void messageCommandHelpFlow()  throws Exception {
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
                        .content(updateBody(C_HELP)))
                .andExpect(status().isOk());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

        assertThat(request).isNotNull();
        assertThat(request.getUrl().encodedPath()).isEqualTo("/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody()).isNotNull();
        String body = request.getBody().utf8();
        assertThat(body).isNotNull();
        assertThat(body).contains(MessageConstants.HELP);
    }

    @Test
    void messageCommandUnknownFlow()  throws Exception {
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
                        .content(updateBody(C_MUSIC)))
                .andExpect(status().isOk());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

        assertThat(request).isNotNull();
        assertThat(request.getUrl().encodedPath()).isEqualTo("/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody()).isNotNull();
        String body = request.getBody().utf8();
        assertThat(body).isNotNull();
        assertThat(body).contains(MessageConstants.DEFAULT);
    }

    @Test
    void sendMessageError500Response() throws Exception {
        server.enqueue(
                new MockResponse.Builder()
                        .code(500)
                        .addHeader("Content-Type", "application/json")
                        .build()
        );
        mockMvc.perform(post("/webhooks/telegram")
                        .header(TelegramHeader.TELEGRAM_HEADER, TELEGRAM_WH_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(TEXT_MESSAGE)))
                .andExpect(status().isInternalServerError());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getUrl().encodedPath()).isEqualTo("/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage");
    }

    @Test
    void sendMessageBodyResponseNull() throws Exception {
        server.enqueue(
                new MockResponse.Builder()
                        .code(200)
                        .addHeader("Content-Type", "application/json")
                        .build()
        );
        mockMvc.perform(post("/webhooks/telegram")
                        .header(TelegramHeader.TELEGRAM_HEADER, TELEGRAM_WH_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(TEXT_MESSAGE)))
                .andExpect(status().isInternalServerError());

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getUrl().encodedPath()).isEqualTo("/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage");
    }





    private String updateBody(String text){
        return """
                {
                    "update_id": 10000,
                    "message": {
                        "message_id": 123,
                        "date": 1234,
                        "text":"%s",
                        "from":{
                            "id":124,
                            "is_bot": true,
                            "first_name": "Emma",
                            "username":"emmatest"
                        },
                        "chat":{
                            "id":1235,
                            "type":"private"
                        }
                    }
                }
                """.formatted(text);
    }



    private String updateTextNullBody(){
        return """
                {
                    "update_id": 10000,
                    "message": {
                        "message_id": 123,
                        "date": 1234,
                        "from":{
                            "id":124,
                            "is_bot": true,
                            "first_name": "Emma",
                            "username":"emmatest"
                        },
                        "chat":{
                            "id":1235,
                            "type":"private"
                        }
                    }
                }
                """;
    }



    private String updateNullMessageBody(){
        return """
                {
                    "update_id": 10000
                }
                """;
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
