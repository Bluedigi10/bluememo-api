package com.bluedigi.bluememo.integration;

import com.bluedigi.bluememo.BluememoApplication;
import com.bluedigi.bluememo.common.domain.ChannelType;
import com.bluedigi.bluememo.identity.application.port.ChannelIdentityResolver;
import com.bluedigi.bluememo.identity.application.service.ChannelAccountService;
import com.bluedigi.bluememo.identity.infrastructure.persistence.entity.UserEntity;
import com.bluedigi.bluememo.identity.infrastructure.persistence.repository.UserJpaRepository;
import com.bluedigi.bluememo.identity.infrastructure.web.request.CreateChannelLinkToken;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class ChannelLinkRestartTest {
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Test
    void tokensActiveLinksAndRevokedHistorySurviveApplicationContextRecreation() {
        UUID activeUser;
        UUID pendingUser;
        UUID revokedUser;
        String consumedToken;
        String pendingToken;
        String revokedToken;
        Instant linkedAt;
        Instant consentedAt;
        Instant revokedAt;

        // Close the entire application (including its entity manager and connection pool),
        // but retain the same PostgreSQL database across three independent contexts.
        try (var first = start()) {
            var links = first.getBean(ChannelAccountService.class);
            activeUser = user(first);
            pendingUser = user(first);
            revokedUser = user(first);
            consumedToken = token(links, activeUser);
            pendingToken = token(links, pendingUser);
            revokedToken = token(links, revokedUser);
            links.linkAccount("active", ChannelType.TELEGRAM, "active-chat", consumedToken);
            links.linkAccount("revoked", ChannelType.TELEGRAM, "revoked-chat", revokedToken);
            links.unlinkChannel(revokedUser.toString(), ChannelType.TELEGRAM);
            var active = links.getLinks(activeUser.toString(), ChannelType.TELEGRAM).get(0);
            linkedAt = active.getLinkedAt();
            consentedAt = active.getConsentedAt();
            revokedAt = links.getLinks(revokedUser.toString(), ChannelType.TELEGRAM).get(0).getRevokedAt();
        }

        try (var second = start()) {
            var links = second.getBean(ChannelAccountService.class);
            var resolver = second.getBean(ChannelIdentityResolver.class);
            assertThat(resolver.resolve(ChannelType.TELEGRAM, "active", "active-chat")).contains(activeUser);
            assertThat(resolver.resolve(ChannelType.TELEGRAM, "revoked", "revoked-chat")).isEmpty();
            var active = links.getLinks(activeUser.toString(), ChannelType.TELEGRAM).get(0);
            assertThat(active.getLinkedAt()).isEqualTo(linkedAt);
            assertThat(active.getConsentedAt()).isEqualTo(consentedAt);
            assertThat(active.getConsentVersion()).isEqualTo("1");
            assertThat(links.getLinks(revokedUser.toString(), ChannelType.TELEGRAM).get(0).getRevokedAt()).isEqualTo(revokedAt);
            assertThat(links.linkAccount("other", ChannelType.TELEGRAM, "other-chat", consumedToken)).contains("inválido");
            assertThat(links.linkAccount("revoked", ChannelType.TELEGRAM, "revoked-chat", revokedToken)).contains("inválido");
            assertThat(links.linkAccount("pending", ChannelType.TELEGRAM, "pending-chat", pendingToken)).contains("éxito");
        }

        try (var third = start()) {
            var links = third.getBean(ChannelAccountService.class);
            assertThat(third.getBean(ChannelIdentityResolver.class)
                    .resolve(ChannelType.TELEGRAM, "pending", "pending-chat")).contains(pendingUser);
            assertThat(links.linkAccount("pending", ChannelType.TELEGRAM, "pending-chat", pendingToken)).contains("inválido");
        }
    }

    private ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(BluememoApplication.class).profiles("test").run(
                "--server.port=0",
                "--spring.datasource.url=" + postgres.getJdbcUrl(),
                "--spring.datasource.username=" + postgres.getUsername(),
                "--spring.datasource.password=" + postgres.getPassword());
    }

    private UUID user(ConfigurableApplicationContext context) {
        UserEntity user = new UserEntity();
        user.setName("Restart test");
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setPassword("unused-test-password");
        return context.getBean(UserJpaRepository.class).saveAndFlush(user).getId();
    }

    private String token(ChannelAccountService links, UUID user) {
        return links.generateLink(new CreateChannelLinkToken(user.toString(), ChannelType.TELEGRAM, true))
                .linkUrl().split("start=", 2)[1];
    }
}
