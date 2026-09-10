package com.bluedigi.bluememo.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class ChannelLinkMigrationTest {
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    private JdbcTemplate jdbc;
    private String schema;

    @BeforeEach
    void migrateToV4() {
        schema = "migration_" + UUID.randomUUID().toString().replace("-", "");
        flyway("4").migrate();
        String jdbcUrl = postgres.getJdbcUrl();
        var source = new DriverManagerDataSource(jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema,
                postgres.getUsername(), postgres.getPassword());
        jdbc = new JdbcTemplate(source);
    }

    @Test
    void v5PreservesLegacyAccountsAndInvalidatesUnusedTokensWithoutInventingConsent() {
        UUID owner = user();
        UUID expiredOwner = user();
        UUID usedOwner = user();
        Instant usedAt = Instant.parse("2026-01-01T00:00:00Z");
        account(owner, "sender", "chat");
        token(owner, "pending-hash", Instant.now().plusSeconds(600), null);
        token(expiredOwner, "expired-hash", Instant.now().minusSeconds(600), null);
        token(usedOwner, "used-hash", Instant.now().plusSeconds(600), usedAt);
        var oldAccount = jdbc.queryForMap("SELECT * FROM channel_accounts");
        var oldTokens = jdbc.queryForList("SELECT id, user_id, token_hash, expires_at FROM channel_link_tokens ORDER BY token_hash");

        var result = flyway("5").migrate();

        assertThat(result.migrationsExecuted).isEqualTo(1);
        assertThat(jdbc.queryForMap("SELECT id, user_id, channel_type, external_user_id, external_chat_id, linked_at FROM channel_accounts"))
                .isEqualTo(oldAccount);
        assertThat(jdbc.queryForList("SELECT id, user_id, token_hash, expires_at FROM channel_link_tokens ORDER BY token_hash")).isEqualTo(oldTokens);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM channel_link_tokens WHERE used_at IS NULL", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT used_at FROM channel_link_tokens WHERE token_hash = 'used-hash'", Timestamp.class).toInstant()).isEqualTo(usedAt);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM channel_accounts WHERE consented_at IS NULL AND consent_version IS NULL AND revoked_at IS NULL", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM channel_link_tokens WHERE consented_at IS NULL AND consent_version IS NULL", Integer.class)).isEqualTo(3);
        assertThat(flyway("5").migrate().migrationsExecuted).isZero();
    }

    @Test
    void activeUniqueIndexesRejectAmbiguityButAllowNewLinksAfterRevocation() {
        UUID owner = user();
        UUID other = user();
        account(owner, "sender", "chat");
        flyway("5").migrate();
        assertThatThrownBy(() -> account(owner, "different", "different-chat"))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        assertThatThrownBy(() -> account(other, "sender", "different-chat"))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        assertThatThrownBy(() -> account(other, "different", "chat"))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        jdbc.update("UPDATE channel_accounts SET revoked_at = CURRENT_TIMESTAMP");
        account(owner, "sender", "chat");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM channel_accounts", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM channel_accounts WHERE revoked_at IS NULL", Integer.class)).isEqualTo(1);
    }

    @Test
    void duplicateLegacyConversationsFailMigrationAndRollBackWithoutDeletingData() {
        UUID first = user();
        UUID second = user();
        account(first, "first", "shared-chat");
        account(second, "second", "shared-chat");
        token(first, "pending-hash", Instant.now().plusSeconds(600), null);
        var before = jdbc.queryForList("SELECT * FROM channel_accounts ORDER BY external_user_id");
        assertThatThrownBy(() -> flyway("5").migrate()).isInstanceOf(FlywayException.class);
        assertThat(jdbc.queryForList("SELECT * FROM channel_accounts ORDER BY external_user_id")).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM channel_link_tokens WHERE used_at IS NULL", Integer.class)).isEqualTo(1);
        assertThat(flyway("4").info().current().getVersion().getVersion()).isEqualTo("4");
    }

    private Flyway flyway(String version) {
        return Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas(schema).locations("classpath:db/migration").target(version).load();
    }

    private UUID user() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, name, email, password) VALUES (?, 'Migration test', ?, 'unused')", id, id + "@example.com");
        return id;
    }

    private void account(UUID user, String sender, String chat) {
        jdbc.update("INSERT INTO channel_accounts (id, user_id, channel_type, external_user_id, external_chat_id, linked_at) VALUES (?, ?, 'TELEGRAM', ?, ?, CURRENT_TIMESTAMP)",
                UUID.randomUUID(), user, sender, chat);
    }

    private void token(UUID user, String hash, Instant expiresAt, Instant usedAt) {
        jdbc.update("INSERT INTO channel_link_tokens (id, user_id, channel_type, token_hash, expires_at, used_at) VALUES (?, ?, 'TELEGRAM', ?, ?, ?)",
                UUID.randomUUID(), user, hash, Timestamp.from(expiresAt), usedAt == null ? null : Timestamp.from(usedAt));
    }
}
