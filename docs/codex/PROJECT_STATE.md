# BlueMemo — Current Project State

## Verified Git baseline — 2026-09-09

- Branch: `feat/BM-03-telegram-user-linking`.
- HEAD: `930500e7825fd81124551db2c1d5b1b7b6e2250a` (`docs: record BM-03 account deletion and unlinking decisions`).
- Commit time: 2026-09-09 06:52:24 UTC / 00:52:24 America/Mexico_City.
- Local main: `ea61c45a1fa0cda23bb840dedb49d4e5f687f790`; HEAD is 16 ahead, 0 behind.
- Working tree was clean at review start. AGENTS.md and docs/codex are now tracked.
- No remote fetch or CI query was performed. Do not claim current CI is green.
- Implementation below includes the current uncommitted BM-03 changes, not only HEAD.

## Closed deliveries and architecture

BM-00, BM-01 (PR #8) and BM-02 (PR #9) remain closed. Existing behavior includes protected Telegram webhook, generic message routing, own RestClient Bot API wrapper, PostgreSQL event idempotency, status lifecycle, and ordered Unicode-safe outbound splitting in the Telegram adapter.

Java 17, Spring Boot 4.1.0, Maven Wrapper, PostgreSQL/JPA, Flyway, MockMvc/MockWebServer, PostgreSQL Testcontainers and GitHub Actions remain the foundation. BM-03 is identity linking only; BM-04 authorization and later integrations remain out of scope.

## Changes already committed since the old snapshot

`3bef44e` changed expiry to 10 minutes, removed the /start token argument from command logging, made missing-token lookup return Optional/normal reply, added explicit account-deletion cleanup, and added a physical unlink endpoint. `930500e` recorded deletion/unlinking decisions.

The old 6932118 snapshot, 15-minute discrepancy, missing-token exception finding and missing unlink-endpoint statement are obsolete. The committed physical unlink did not preserve history or invalidate pending tokens, and UserServiceTest still used the old constructor.

## Current BM-03 working implementation

- `POST /users/me/link/channel/{channelType}` requires JWT ownership and JSON `{"consent":true}`. The user approved keeping this route rather than the historical challenges route.
- Generation rejects an active user/channel link with 409, generates 256 random bits, stores SHA-256 only and returns the deep link plus `expirationDate` (10 minutes).
- Consent version `1` and `consentedAt` are recorded with the token and copied to the verified association. Consent covers channel identity linking, not Tools authorization.
- `/start <token>` accepts linking only in a private conversation. Telegram chat.type is interpreted in its adapter and represented by a channel-independent privateConversation boolean.
- Missing/expired/replaced/used tokens receive normal invalid-token replies. Command logs contain only normalized enum values.
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

UserServiceTest was updated for cleanup dependencies and ordering. Final test results are recorded at the end of this document once verification finishes. No real dev-bot E2E or remote CI validation has been performed in this review. BM-03 is not declared closed.

Remaining release evidence includes dev-bot E2E, CI for the final commit, and any additional acceptance cases not covered by the checked-in suites (for example restart/context recreation and migration against representative pre-existing data).

The branch also retains an earlier unrelated Todo persistence-package spelling/format change; this work does not expand it.

## Verification result — 2026-09-09

- Focused UserServiceTest + ChannelLinkIntegrationTest: 25 tests, 0 failures/errors/skips.
- `mvnw.cmd --batch-mode --no-transfer-progress clean verify`: BUILD SUCCESS, 103 tests, 0 failures/errors/skips (including 13 dedicated BM-03 integration tests), PostgreSQL 17 via Testcontainers.
- The first integration attempt could not find Docker; after starting Docker Desktop, both the focused and full runs passed.
- After the successful run only import ordering/whitespace and documentation were adjusted; no behavioral changes were made.
- No CI run, deployment or real Telegram dev-bot E2E was performed. Changes remain uncommitted.
