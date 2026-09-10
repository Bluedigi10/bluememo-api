package com.bluedigi.bluememo.integration;

import com.bluedigi.bluememo.common.domain.ChannelType;
import com.bluedigi.bluememo.common.infrastructure.security.JwtService;
import com.bluedigi.bluememo.identity.application.port.ChannelIdentityResolver;
import com.bluedigi.bluememo.identity.application.service.ChannelAccountService;
import com.bluedigi.bluememo.identity.application.service.UserService;
import com.bluedigi.bluememo.identity.domain.model.User;
import com.bluedigi.bluememo.identity.infrastructure.persistence.entity.UserEntity;
import com.bluedigi.bluememo.identity.infrastructure.persistence.repository.UserJpaRepository;
import com.bluedigi.bluememo.identity.infrastructure.web.request.CreateChannelLinkToken;
import com.bluedigi.bluememo.identity.infrastructure.web.request.DeleteUserRequest;
import com.bluedigi.bluememo.testsupport.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import com.bluedigi.bluememo.messaging.application.port.out.SendMessagePort;
import com.bluedigi.bluememo.messaging.domain.OutgoingMessage;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@IntegrationTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ChannelLinkIntegrationTest {
    private static final ChannelType CHANNEL = ChannelType.TELEGRAM;
    @Autowired ChannelAccountService links;
    @Autowired ChannelIdentityResolver resolver;
    @Autowired UserService users;
    @Autowired UserJpaRepository userRepository;
    @Autowired PasswordEncoder passwords;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockitoBean SendMessagePort sender;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM incoming_events");
        jdbc.update("DELETE FROM channel_accounts");
        jdbc.update("DELETE FROM channel_link_tokens");
        jdbc.update("DELETE FROM todos");
        jdbc.update("DELETE FROM users");
    }

    @Test
    void webhookRequiresPrivateChatDoesNotLeakTokenAndDeduplicates(CapturedOutput output) throws Exception {
        UUID user = user();
        String token = token(user);
        webhook(1, "group", token);
        assertThat(count("channel_accounts")).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM channel_link_tokens WHERE used_at IS NULL", Integer.class)).isEqualTo(1);
        webhook(2, "private", token);
        webhook(2, "private", token);
        assertThat(resolver.resolve(CHANNEL, "123", "456")).contains(user);
        var replies = ArgumentCaptor.forClass(OutgoingMessage.class);
        verify(sender, times(2)).send(replies.capture());
        assertThat(replies.getAllValues().get(0).text()).contains("privada");
        assertThat(replies.getAllValues().get(1).text()).contains("éxito");
        assertThat(output.getAll()).doesNotContain(token);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM incoming_events WHERE status = 'ANSWERED'", Integer.class)).isEqualTo(2);
    }

    @Test
    void unknownTokenGetsNormalWebhookReply() throws Exception {
        webhook(3, "private", "unknown-token");
        var reply = ArgumentCaptor.forClass(OutgoingMessage.class);
        verify(sender).send(reply.capture());
        assertThat(reply.getValue().text()).contains("inválido");
        assertThat(jdbc.queryForObject("SELECT status FROM incoming_events", String.class)).isEqualTo("ANSWERED");
    }

    private void webhook(long event, String chatType, String token) throws Exception {
        command(event, chatType, "/start " + token, 123, 456);
    }

    private void command(long event, String chatType, String text, long senderId, long chatId) throws Exception {
        String body = """
                {"update_id":%d,"message":{"message_id":1,"date":1788937200,
                "text":"%s","from":{"id":%d},"chat":{"id":%d,"type":"%s"}}}
                """.formatted(event, text, senderId, chatId, chatType);
        mvc.perform(post("/webhooks/telegram").header("X-Telegram-Bot-Api-Secret-Token", "test-webhook-secret")
                .contentType("application/json").content(body)).andExpect(status().isOk());
    }

    @Test
    void checkLinkReflectsLinkRevocationAndFreshLinkWithoutExposingIdentity(CapturedOutput output) throws Exception {
        UUID user = user();
        command(10, "private", "/check-link", 123, 456);
        String firstToken = token(user);
        webhook(11, "private", firstToken);
        command(12, "private", "/check-link", 123, 456);
        command(12, "private", "/check-link", 123, 456);
        links.unlinkChannel(user.toString(), CHANNEL);
        command(13, "private", "/check-link", 123, 456);
        String freshToken = token(user);
        webhook(14, "private", freshToken);
        command(15, "private", "/check-link", 123, 456);

        var replies = ArgumentCaptor.forClass(OutgoingMessage.class);
        verify(sender, times(6)).send(replies.capture());
        var texts = replies.getAllValues().stream().map(OutgoingMessage::text).toList();
        assertThat(texts.get(0)).isEqualTo("Tu cuenta de Telegram no está vinculada a BlueMemo.");
        assertThat(texts.get(2)).isEqualTo("Tu cuenta de Telegram está vinculada a BlueMemo.");
        assertThat(texts.get(3)).isEqualTo(texts.get(0));
        assertThat(texts.get(5)).isEqualTo(texts.get(2));
        assertThat(texts).allSatisfy(text -> assertThat(text).doesNotContain(user.toString(), firstToken, freshToken));
        assertThat(output.getAll()).doesNotContain(firstToken, freshToken);
        assertThat(count("channel_accounts")).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM incoming_events WHERE status = 'ANSWERED'", Integer.class)).isEqualTo(6);
    }

    @Test
    void checkLinkRequiresMatchingSenderAndConversationAndDoesNotConsumePendingToken() throws Exception {
        links.linkAccount("123", CHANNEL, "456", token(user()));
        UUID other = user();
        token(other);
        command(20, "private", "/check-link", 123, 789);
        command(21, "private", "/check-link", 789, 456);
        var replies = ArgumentCaptor.forClass(OutgoingMessage.class);
        verify(sender, times(2)).send(replies.capture());
        assertThat(replies.getAllValues()).allSatisfy(reply ->
                assertThat(reply.text()).isEqualTo("Tu cuenta de Telegram no está vinculada a BlueMemo."));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM channel_link_tokens WHERE used_at IS NULL", Integer.class)).isEqualTo(1);
        assertThat(resolver.resolve(CHANNEL, "123", "456")).isPresent();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"group", "supergroup", "channel"})
    void checkLinkDoesNotRevealStatusOutsidePrivateChat(String chatType) throws Exception {
        links.linkAccount("123", CHANNEL, "456", token(user()));
        command(30, chatType, "/check-link", 123, 456);
        var reply = ArgumentCaptor.forClass(OutgoingMessage.class);
        verify(sender).send(reply.capture());
        assertThat(reply.getValue().text()).isEqualTo("Este comando solo está disponible en una conversación privada");
        assertThat(count("channel_accounts")).isEqualTo(1);
    }

    @Test
    void creationRequiresAuthenticationAndExplicitConsent() throws Exception {
        String url = "/users/me/link/channel/TELEGRAM";
        mvc.perform(post(url).contentType("application/json").content("{\"consent\":true}"))
                .andExpect(status().isUnauthorized());
        UUID user = user();
        String auth = auth(user);
        for (String body : new String[]{"{}", "{\"consent\":false}", "{\"consent\":null}"}) {
            mvc.perform(post(url).header("Authorization", auth).contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
        assertThat(count("channel_link_tokens")).isZero();
        mvc.perform(post(url).header("Authorization", auth).contentType("application/json").content("{\"consent\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.linkUrl").isString());
        assertThat(count("channel_link_tokens")).isEqualTo(1);
    }

    @Test
    void tokenIsHashOnlyExpiresInTenMinutesAndConsentSurvivesLinking() {
        UUID user = user();
        Instant before = Instant.now();
        var response = links.generateLink(new CreateChannelLinkToken(user.toString(), CHANNEL, true));
        String token = response.linkUrl().split("start=", 2)[1];
        assertThat(java.util.Base64.getUrlDecoder().decode(token)).hasSize(32);
        assertThat(response.expirationDate()).isBetween(before.plusSeconds(600), Instant.now().plusSeconds(600));
        assertThat(jdbc.queryForObject("SELECT token_hash FROM channel_link_tokens", String.class)).isNotEqualTo(token);
        assertThat(links.linkAccount("sender", CHANNEL, "chat", token)).contains("éxito");
        assertThat(resolver.resolve(CHANNEL, "sender", "chat")).contains(user);
        assertThat(resolver.resolve(CHANNEL, "sender", "different-chat")).isEmpty();
        assertThat(resolver.resolve(CHANNEL, "different-sender", "chat")).isEmpty();
        var account = links.getLinks(user.toString(), CHANNEL).get(0);
        assertThat(account.getConsentedAt()).isNotNull();
        assertThat(account.getConsentVersion()).isEqualTo("1");
        assertThat(links.linkAccount("sender", CHANNEL, "chat", token)).contains("inválido");
        assertThat(count("channel_accounts")).isEqualTo(1);
    }

    @Test
    void missingExpiredAndReplacedTokensCannotLink() {
        UUID user = user();
        assertThat(links.linkAccount("sender", CHANNEL, "chat", "unknown")).contains("inválido");
        String replaced = token(user);
        String current = token(user);
        assertThat(links.linkAccount("sender", CHANNEL, "chat", replaced)).contains("inválido");
        jdbc.update("UPDATE channel_link_tokens SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second'");
        assertThat(links.linkAccount("sender", CHANNEL, "chat", current)).contains("inválido");
        assertThat(count("channel_accounts")).isZero();
    }

    @Test
    void unlinkPreservesHistoryInvalidatesPendingTokenAndAllowsFreshFlow() {
        UUID user = user();
        links.linkAccount("sender", CHANNEL, "chat", token(user));
        links.unlinkChannel(user.toString(), CHANNEL);
        assertThat(resolver.resolve(CHANNEL, "sender", "chat")).isEmpty();
        assertThat(links.getLinks(user.toString(), CHANNEL).get(0).getRevokedAt()).isNotNull();
        String pending = token(user);
        links.unlinkChannel(user.toString(), CHANNEL);
        assertThat(links.linkAccount("new-sender", CHANNEL, "new-chat", pending)).contains("inválido");
        assertThat(links.linkAccount("new-sender", CHANNEL, "new-chat", token(user))).contains("éxito");
        assertThat(count("channel_accounts")).isEqualTo(2);
        assertThat(resolver.resolve(CHANNEL, "new-sender", "new-chat")).contains(user);
        assertThat(count("users")).isEqualTo(1);
    }

    @Test
    void statusAndUnlinkOnlyAffectAuthenticatedOwner() throws Exception {
        UUID owner = user();
        UUID other = user();
        links.linkAccount("sender", CHANNEL, "chat", token(owner));
        mvc.perform(get("/users/me/link/channel/TELEGRAM").header("Authorization", auth(other)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(delete("/users/me/unlink/channel/TELEGRAM").header("Authorization", auth(other)))
                .andExpect(status().isNoContent());
        assertThat(resolver.resolve(CHANNEL, "sender", "chat")).contains(owner);
        mvc.perform(delete("/users/me/unlink/channel/TELEGRAM").header("Authorization", auth(owner)))
                .andExpect(status().isNoContent());
        assertThat(resolver.resolve(CHANNEL, "sender", "chat")).isEmpty();
    }

    @Test
    void sameTokenConcurrentConsumptionCreatesOnlyOneLink() throws Exception {
        UUID user = user();
        String token = token(user);
        race(() -> links.linkAccount("sender", CHANNEL, "chat", token),
             () -> links.linkAccount("sender", CHANNEL, "chat", token));
        assertThat(count("channel_accounts")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM channel_link_tokens WHERE used_at IS NOT NULL", Integer.class)).isEqualTo(1);
    }

    @Test
    void competingExternalOwnershipConsumesBothTokensWithoutRollback() throws Exception {
        String first = token(user());
        String second = token(user());
        race(() -> links.linkAccount("sender", CHANNEL, "chat", first),
             () -> links.linkAccount("sender", CHANNEL, "chat", second));
        assertThat(count("channel_accounts")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM channel_link_tokens WHERE used_at IS NOT NULL", Integer.class)).isEqualTo(2);
    }

    @Test
    void conversationConflictConsumesTokenAndCannotCreateAmbiguousIdentity() {
        links.linkAccount("first", CHANNEL, "shared-chat", token(user()));
        String second = token(user());
        assertThat(links.linkAccount("second", CHANNEL, "shared-chat", second)).contains("vinculación activa");
        assertThat(count("channel_accounts")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM channel_link_tokens WHERE used_at IS NOT NULL", Integer.class)).isEqualTo(2);
    }

    @Test
    void deletionRemovesActiveAndRevokedHistoryAndTokens() {
        UUID user = user();
        links.linkAccount("old", CHANNEL, "old-chat", token(user));
        links.unlinkChannel(user.toString(), CHANNEL);
        links.linkAccount("new", CHANNEL, "new-chat", token(user));
        users.deleteUserById(user.toString(), new DeleteUserRequest("password123"));
        assertThat(count("channel_accounts")).isZero();
        assertThat(count("channel_link_tokens")).isZero();
        assertThat(count("users")).isZero();
    }

    @Test
    void deletionAlsoRemovesOutstandingAndExpiredTokens() {
        for (boolean expired : new boolean[]{false, true}) {
            UUID user = user();
            token(user);
            if (expired) jdbc.update("UPDATE channel_link_tokens SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 day'");
            users.deleteUserById(user.toString(), new DeleteUserRequest("password123"));
            assertThat(count("channel_link_tokens")).isZero();
            assertThat(count("users")).isZero();
        }
    }

    @Test
    void cleanupFailureRollsBackPreviouslyDeletedAssociations() {
        UUID user = user();
        String userString = user.toString();
        DeleteUserRequest deleteRequest = new DeleteUserRequest("password123");
        links.linkAccount("sender", CHANNEL, "chat", token(user));
        jdbc.execute("CREATE FUNCTION reject_token_cleanup() RETURNS trigger LANGUAGE plpgsql AS 'BEGIN RAISE EXCEPTION ''test cleanup failure''; END'");
        jdbc.execute("CREATE TRIGGER reject_token_cleanup BEFORE DELETE ON channel_link_tokens FOR EACH ROW EXECUTE FUNCTION reject_token_cleanup()");
        try {
            assertThatThrownBy(() -> users.deleteUserById(userString, deleteRequest))
                    .isInstanceOf(RuntimeException.class);
            assertThat(count("users")).isEqualTo(1);
            assertThat(count("channel_link_tokens")).isEqualTo(1);
            assertThat(resolver.resolve(CHANNEL, "sender", "chat")).contains(user);
        } finally {
            jdbc.execute("DROP TRIGGER reject_token_cleanup ON channel_link_tokens");
            jdbc.execute("DROP FUNCTION reject_token_cleanup()");
        }
    }

    private UUID user() {
        UserEntity user = new UserEntity();
        user.setName("Test");
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setPassword(passwords.encode("password123"));
        return userRepository.saveAndFlush(user).getId();
    }

    private String auth(UUID id) {
        User user = new User();
        user.setId(id);
        user.setName("Test");
        user.setEmail("test@example.com");
        return "Bearer " + jwt.generateToken(user);
    }

    private String token(UUID user) {
        return links.generateLink(new CreateChannelLinkToken(user.toString(), CHANNEL, true)).linkUrl().split("start=", 2)[1];
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private void race(Runnable first, Runnable second) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var a = executor.submit(() -> { await(ready, start); first.run(); });
            var b = executor.submit(() -> { await(ready, start); second.run(); });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            a.get(15, TimeUnit.SECONDS);
            b.get(15, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private void await(CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("Concurrent start timed out");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
