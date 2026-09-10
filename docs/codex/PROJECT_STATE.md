# BlueMemo — Current Project State

## Verified Git baseline — 2026-09-10

- Branch: `feat/BM-03-telegram-user-linking`.
- Verified base commit: `657fb6093e0fd89f716348b30b9d6de55b61e737` (`minor change`). Additional tests were validated in the working tree before commit; subsequent documentation updates were checked with `git diff --check`. This identifies the reviewed baseline, not necessarily the latest HEAD after future commits.
- Commit time: 2026-09-10 06:50:09 UTC / 00:50:09 America/Mexico_City.
- Local main: `ea61c45a1fa0cda23bb840dedb49d4e5f687f790`; HEAD is 21 ahead, 0 behind.
- The initial HEAD validation began with a clean tree. The current working tree includes 20 additional test cases and documentation updates, still uncommitted.
- No remote fetch or CI query was performed in this documentation update. The user reports successful PR #10 CI for 0217b00; this does not establish CI success for current HEAD 657fb60 or the uncommitted tests.
- The previous implementation was committed in `9617118`; current implementation below is present in HEAD. The local origin tracking reference also points to HEAD; no fetch was performed.

## Closed deliveries and architecture

BM-00, BM-01 (PR #8) and BM-02 (PR #9) remain closed. Existing behavior includes protected Telegram webhook, generic message routing, own RestClient Bot API wrapper, PostgreSQL event idempotency, status lifecycle, and ordered Unicode-safe outbound splitting in the Telegram adapter.

Java 17, Spring Boot 4.1.0, Maven Wrapper, PostgreSQL/JPA, Flyway, MockMvc/MockWebServer, PostgreSQL Testcontainers and GitHub Actions remain the foundation. BM-03 is identity linking only; BM-04 authorization and later integrations remain out of scope.

## Changes already committed since the old snapshot

`3bef44e` changed expiry to 10 minutes, removed the /start token argument from command logging, made missing-token lookup return Optional/normal reply, added explicit account-deletion cleanup, and added a physical unlink endpoint. `930500e` recorded deletion/unlinking decisions.

The old 6932118 snapshot, 15-minute discrepancy, missing-token exception finding and missing unlink-endpoint statement are obsolete. The committed physical unlink did not preserve history or invalidate pending tokens, and UserServiceTest still used the old constructor.

## Current BM-03 implementation

- `POST /users/me/link/channel/{channelType}` requires JWT ownership and JSON `{"consent":true}`. The user approved keeping this route rather than the historical challenges route.
- Generation rejects an active user/channel link with 409, generates 256 random bits, stores SHA-256 only and returns the deep link plus `expirationDate` (10 minutes).
- Consent version `1` and `consentedAt` are recorded with the token and copied to the verified association. Consent covers channel identity linking, not Tools authorization.
- `/start <token>` accepts linking only in a private conversation. Telegram chat.type is interpreted in its adapter and represented by a channel-independent privateConversation boolean.
- Missing/expired/replaced/used tokens receive normal invalid-token replies. The Telegram adapter now converts commands into IncomingAction values; generic processing logs only these normalized actions.
- Token consumption remains a conditional SQL update before ownership checks. Account insertion uses `ON CONFLICT DO NOTHING` so ownership races produce controlled replies and commit consumption rather than rolling it back.
- A PostgreSQL user-row lock coordinates generation, linking, unlinking and account deletion for each BlueMemo user. External ownership/conversation uniqueness remains database-enforced across users.
- `DELETE /users/me/unlink/channel/{channelType}` revokes active associations and invalidates outstanding channel tokens, preserving the user, other channels and history. Re-linking requires a fresh verified token.
- `GET /users/me/link/channel/{channelType}` returns only the authenticated user's association history; revokedAt distinguishes revoked from active entries.
- `ChannelIdentityResolver` resolves an Optional BlueMemo UUID only when channel, external user and conversation all match an active association.
- Account deletion explicitly deletes all associations/history and all tokens, then todos and user in one transaction.

## Persistence and migration

V4 is unchanged. V5 adds revoked_at and consent fields, replaces unconditional account ownership constraints with active-only unique indexes for user, external user and conversation, and invalidates unused pre-consent tokens.

Existing associations retain null consent fields: migration does not fabricate historical consent. Existing active associations remain active. If existing data has duplicate active conversations, V5 fails rather than silently deleting or reassigning ownership; inspect actual deployed data before applying it.

Revoked associations retain linkedAt, revokedAt, consentedAt and consentVersion. Token upsert still replaces the previous token for a user/channel; it is not an issuance-history ledger.

## Verification and remaining work

Dedicated ChannelLinkIntegrationTest covers consent/authentication, hash-only storage/expiry, invalid and replaced tokens, private-chat webhook/deduplication, safe logging, resolution consistency, owner isolation, revocation/fresh linking, same-token and external-ownership races, conversation conflicts, account cleanup and rollback on cleanup failure.

UserServiceTest covers cleanup dependencies and ordering. The latest local full run passed 123 tests. The user has now recorded real DEV bot E2E and application-restart evidence below. BM-03 core functionality is verified; formal closure remains pending PR review resolution and CI for the final HEAD.

Remaining closure work is to resolve or confirm resolution of PR review findings and obtain CI for the final commit, including the additional tests. Real DEV bot E2E and application restart are no longer pending. Automated migration fixtures do not replace review of data in a different deployment environment. Application-context recreation and V4-to-V5 migration against representative fixtures now have automated coverage (see the testing update below).

The branch also retains an earlier unrelated Todo persistence-package spelling/format change; this work does not expand it.

## Verification result — 2026-09-09

- Focused UserServiceTest + ChannelLinkIntegrationTest: 25 tests, 0 failures/errors/skips.
- `mvnw.cmd --batch-mode --no-transfer-progress clean verify`: BUILD SUCCESS, 103 tests, 0 failures/errors/skips (including 13 dedicated BM-03 integration tests), PostgreSQL 17 via Testcontainers.
- The first integration attempt could not find Docker; after starting Docker Desktop, both the focused and full runs passed.
- After the successful run only import ordering/whitespace and documentation were adjusted; no behavioral changes were made.
- Historical result: no CI run, deployment or real Telegram dev-bot E2E was performed during that review. Those changes were subsequently committed in `9617118`.

## Changes since the previous review — 2026-09-10

- `9617118` committed consent, revocation/history, resolver, V5, cleanup and the BM-03 tests.
- `5e19ebc` ignores IntelliJ run configurations.
- `0217b00`, `de09824` and `657fb60` add/refine the linking-status command and Telegram action adaptation. The implemented command is `/check-link` (the earlier commit title says `/check-list`).
- TelegramCommands and TelegramActionConverter now belong to the Telegram adapter. IncomingMessage carries a generic IncomingAction; IncomingEventStatus moved to messaging.domain.enums.
- `/check-link` is private-chat-only and checks channel + sender + conversation through ChannelIdentityResolver. It returns linked/unlinked status without exposing the BlueMemo UUID.
- ProcessIncomingMessageService now catches processing failures as well as outbound failures and attempts to mark the event FAILED.
- README documents TELEGRAM_BOT_NAME and now includes `/check-link` usage.
- At the initial review there were no dedicated assertions for `/check-link` or TelegramActionConverter. These gaps are covered by the subsequent testing update below.
- The generic check-link response still hardcodes the word Telegram; protocol parsing has moved out of the core, but this presentation detail remains channel-specific.
- BM-03 remains active. This initial repository review preceded the user-reported real bot/restart evidence recorded below.

## Verification result — 2026-09-10, HEAD 657fb60

`mvnw.cmd --batch-mode --no-transfer-progress clean verify` completed with BUILD SUCCESS: 103 tests, zero failures, errors or skips, including PostgreSQL Testcontainers integration coverage. `git diff --check` passed. No production source or tests were modified during this validation; only this snapshot document was refreshed. At that initial review, remote CI and real-bot E2E had not been verified; see the subsequently supplied evidence below.

## Added BM-03 coverage — 2026-09-10 (uncommitted)

- TelegramActionConverterTest: 11 parameterized cases for plain/empty/null text, welcome, link tokens with whitespace, complete token payload, help, check-link and unknown commands.
- ChannelLinkIntegrationTest: 5 additional cases for check-link before/after linking/revocation/fresh linking, duplicate-event protection, no identity/token exposure, sender/conversation consistency and refusal in group/supergroup/channel chats.
- ChannelLinkRestartTest: closes and recreates three full application contexts against one PostgreSQL database; verifies pending token usability, persistent consumption, active resolution, revoked history and consent timestamps/version. This recreates the application context, not the database server or operating-system process.
- ChannelLinkMigrationTest: 3 cases start at actual Flyway V4, migrate representative existing data to V5, preserve historical fields without fabricated consent, invalidate legacy tokens, enforce active-only uniqueness, permit re-linking after revocation, and verify rollback when pre-existing conversations conflict.
- Focused run: 33 cases passed, zero failures/errors/skips. These changes add 20 cases to the previous 103-case suite.
- Production sources, migrations and approved decisions are unchanged. No commit was created. Real Telegram bot E2E and remote CI are not replaced by these automated tests.

Final full run: clean verify completed with BUILD SUCCESS, 123 tests, zero failures/errors/skips. git diff --check passed. Test log: bluememo/target/bm03-tests-verify.log (ignored build output).

## BM-03 — Verification evidence

### Automated verification

- Local Maven verification completed successfully:
    - `123 tests`
    - `0 failures`
    - `0 errors`
    - `0 skipped`
- This latest local result includes the uncommitted additions: 18 ChannelLinkIntegrationTest cases, 3 migration cases, 1 application-context recreation case, and 11 converter cases. The earlier HEAD-only run passed 103 tests; these are different tested revisions.
- Covered automated cases include:
    - authenticated link generation;
    - explicit consent;
    - hash-only token persistence;
    - 10-minute expiration;
    - invalid / expired / already-used / replaced tokens;
    - private-chat-only linking;
    - webhook idempotency;
    - ownership isolation;
    - active identity resolution;
    - revocation and relinking;
    - same-token concurrency;
    - external-account ownership conflicts;
    - conversation conflicts;
    - account cleanup;
    - transaction rollback when cleanup fails.
- Per the user-supplied project context, PR #10 CI completed successfully for commit `0217b008aa5aa2a483c8afe302b1513efd8660c6`.
- The same user-supplied record identifies GitHub Actions run #27 with `conclusion: success`. This update did not independently query that run. It predates current HEAD and the additional uncommitted tests.

### Real Telegram DEV E2E verification

The user reports manually testing the BM-03 linking flow against the real Telegram development bot (`BluememoDev` / `@BlueMemoAppDevBot`) and the local BlueMemo API/PostgreSQL database.

The tested commit and exact execution time were not specified in the supplied manual record. The following sequence is retained as user-reported DEV evidence:

1. Before linking:
    - `/check-link` returned that the Telegram account was not linked.
    - `GET /users/me/link/channel/TELEGRAM` returned no active/history entry initially.

2. Link generation:
    - `POST /users/me/link/channel/TELEGRAM` successfully generated a Telegram deep link.
    - A `channel_link_tokens` row was persisted.
    - The persisted value was a hash, not the raw token.
    - `used_at` was initially `NULL`.
    - `expires_at`, `consented_at` and `consent_version = 1` were persisted.

3. Telegram link completion:
    - Opening the generated link triggered `/start <token>` in the real Telegram bot.
    - The bot returned `Cuenta vinculada con éxito. ¡Bienvenido!`.
    - `channel_link_tokens.used_at` was populated after successful consumption.
    - A new active `channel_accounts` row was persisted with:
        - `channel_type = TELEGRAM`;
        - `external_user_id`;
        - `external_chat_id`;
        - `linked_at`;
        - `revoked_at = NULL`;
        - `consented_at`;
        - `consent_version = 1`.
    - `/check-link` then returned that the Telegram account was linked.

4. Link inspection:
    - `GET /users/me/link/channel/TELEGRAM` returned the persisted association and consent/audit fields.

5. Revocation:
    - `DELETE /users/me/unlink/channel/TELEGRAM` returned `204 No Content`.
    - The existing `channel_accounts` row was preserved instead of deleted.
    - `revoked_at` was populated.
    - `/check-link` immediately returned that the Telegram account was no longer linked.
    - The revoked association therefore stopped resolving as an active identity.

6. Pending-token invalidation:
    - A new link token was generated while unlinked.
    - Before use, its `used_at` value was `NULL`.
    - Unlink/revocation was executed before consuming it.
    - The pending token was invalidated and `used_at` was populated.
    - This confirms that a previously issued pending token cannot bypass revocation and restore the association.

7. Fresh relinking:
    - A new token was generated after revocation.
    - `/start <new token>` successfully linked the Telegram account again.
    - `/check-link` returned linked.
    - A second `channel_accounts` row was created as active.
    - The previous row remained preserved with `revoked_at != NULL`.
    - This verifies retained link/revocation history and active-only uniqueness.

8. Link history:
    - `GET /users/me/link/channel/TELEGRAM` returned both associations:
        - newest association active (`revokedAt = null`);
        - previous association revoked (`revokedAt != null`).
    - Results were ordered by `linkedAt` descending.

9. Persistence after restart:
    - The BlueMemo application was stopped and started again.
    - Flyway successfully validated 5 migrations.
    - Database schema reported version `5` and `Schema is up to date`.
    - After restart, `GET /users/me/link/channel/TELEGRAM` still returned both the active and revoked associations.
    - This confirms persistence of BM-03 identity state across application restart.

### Current verification conclusion

BM-03 core functionality has been verified through:
- automated unit/integration tests;
- PostgreSQL/Testcontainers;
- local full Maven verification;
- real Telegram DEV E2E;
- link generation and consumption;
- identity resolution;
- revocation;
- pending-token invalidation;
- fresh relinking;
- retained history;
- application restart/persistence;
- successful PR CI reported for 0217b00 (not the final HEAD).

These successful tests do not supersede unresolved PR review findings. Architectural/reliability review comments should still be addressed and CI rerun against the final PR HEAD before merge.
