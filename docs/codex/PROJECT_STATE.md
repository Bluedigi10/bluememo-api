# BlueMemo — Current Project State

Snapshot generated from repository `Bluedigi10/bluememo-api`.

## Snapshot

| Item | Value |
| --- | --- |
| Active branch | `feat/BM-03-telegram-user-linking` |
| HEAD | `69321186830881bd6b8a408dc8299d137476768c` |
| HEAD message | `minor changes` |
| HEAD time | 2026-09-09 00:53:17 UTC / 2026-09-08 18:53:17 America/Mexico_City |
| Main/base | `ea61c45a1fa0cda23bb840dedb49d4e5f687f790` |
| Branch relation | 14 commits ahead, 0 behind `main` |
| Active delivery | BM-03 — Telegram ↔ BlueMemo user linking |
| CI for snapshot HEAD | No commit status/workflow run found; **not verified green** |

If the current HEAD differs from this value, inspect the new commits and refresh this document before treating the implementation notes below as exact.

## Closed deliveries

### BM-01 — Telegram inbound/outbound functional — CLOSED
Merged through PR #8.

Implemented baseline includes:
- public `POST /webhooks/telegram`;
- Telegram webhook-secret validation;
- mapping Telegram updates to channel-independent messages;
- generic processing and channel routing;
- outbound Telegram Bot API calls via BlueMemo's own `RestClient` wrapper;
- supported text/basic commands and safe handling of unsupported updates;
- integration coverage with MockMvc/MockWebServer.

### BM-02 — Telegram reliability — CLOSED
Merged through PR #9 at `ea61c45`.

Implemented baseline includes:
- persistent incoming-event registration;
- idempotency on `(channel_type, external_event_id)` using PostgreSQL;
- atomic duplicate protection;
- incoming-event status lifecycle;
- Telegram outbound splitting above 4096 Unicode code points;
- code-point-aware splitting that does not cut surrogate pairs;
- ordered sequential fragment delivery;
- failure handling/status update when outbound sending fails;
- Testcontainers-based persistence/integration foundation.

Do not re-scope BM-01/BM-02 while completing BM-03.

## Active implementation: BM-03

BM-03 is **partially implemented**, not done.

### Components currently present

Identity module:
- `identity/application/service/ChannelAccountService`
- `identity/config/LinkProperties`
- `identity/domain/model/ChannelAccount`
- `identity/domain/model/ChannelLinkToken`
- `identity/domain/repository/ChannelAccountRepository`
- `identity/domain/repository/ChannelLinkTokenRepository`
- persistence adapters/entities/JPA repositories/mappers
- `identity/infrastructure/web/ChannelAccountController`
- `CreateChannelLinkToken`
- `LinkChannelResponse`

Shared/messaging changes:
- `ChannelType` moved from `messaging.domain` to `common.domain`.
- `MessageCommands` contains `/start`, `/help`, and fallback `UNKNOWN`.
- `ProcessIncomingMessageService` parses `/start <token>` and delegates linking to `ChannelAccountService`.
- `IncomingMessage` persists external sender and conversation separately (`senderId`, `conversationId`).

Persistence:
- Flyway `V4__create_linkin_tables.sql` creates `channel_link_tokens` and `channel_accounts`.

Configuration:
- `bluememo.link.telegram-url=https://t.me/`
- `bluememo.link.telegram-bot-name=${TELEGRAM_BOT_NAME}`
- Docker Compose now passes `TELEGRAM_BOT_NAME`.

### Current HTTP flow

Implemented endpoint:

```text
POST /users/me/link/channel/{channelType}
```

The controller derives the BlueMemo user from `@AuthenticationPrincipal`; the client does not provide a Telegram identity as proof of ownership.

Current generation behavior:
1. Check whether `(userId, channelType)` is already linked.
2. If linked, throw a controlled `409 CONFLICT` with `Ya tienes una cuenta vinculada a este canal`.
3. Generate 32 random bytes with `SecureRandom` (256 bits).
4. Base64URL-encode without padding.
5. Hash the link token with SHA-256 and persist only the hash.
6. Current code sets expiration to **15 minutes**.
7. Build a Telegram deep link using configured bot name.
8. Upsert the token by `(user_id, channel_type)`.

The latest commit `6932118` specifically added:
- `TELEGRAM_BOT_NAME` to Compose;
- `CONFLICT(409)` to `StatusCodeError`;
- prevention of generating a new link URL when the user already has that channel linked.

### Current `/start <token>` flow

`ProcessIncomingMessageService`:
1. parses commands using whitespace split with a maximum of two parts;
2. `/start` without token returns the normal welcome message;
3. `/start <token>` passes:
   - `senderId` as external Telegram user id;
   - `conversationId` as external Telegram chat id;
   - channel type;
   - raw token;
   to `ChannelAccountService.linkAccount(...)`.

`ChannelAccountService.linkAccount(...)` currently:
1. hashes the supplied token;
2. loads the persisted token by hash;
3. checks `usedAt == null` and `expiresAt > now` via the domain model;
4. checks that the token's channel matches the message channel;
5. atomically marks the token used only when unused and unexpired;
6. only **after consuming the token**, checks whether the BlueMemo user/channel or Telegram external user/channel is already linked;
7. saves `ChannelAccount` if no conflict exists.

The consume-before-link-conflict ordering is intentional; see `DECISIONS.md`.

### Database behavior currently implemented

`channel_link_tokens`:
- PK `id`;
- FK `user_id -> users(id)`;
- `channel_type`;
- unique `token_hash`;
- `expires_at`;
- nullable `used_at`;
- unique `(user_id, channel_type)`.

`ChannelLinkTokenJpaRepository.upsert(...)` uses the `(user_id, channel_type)` constraint. Generating a replacement token for an unlinked account overwrites the previous hash/expiration and resets `used_at` to `NULL`.

`markTokenAsUsed(...)` is atomic:
- token hash must match;
- `used_at IS NULL`;
- `expires_at > CURRENT_TIMESTAMP`;
- one updated row means successful consumption.

`channel_accounts`:
- PK `id`;
- FK `user_id -> users(id)`;
- `channel_type`;
- `external_user_id`;
- `external_chat_id`;
- `linked_at` generated on creation;
- unique `(channel_type, user_id)`;
- unique `(external_user_id, channel_type)`.

There is currently no active/revoked status, `revoked_at`, consent/version metadata, or uniqueness on `(external_chat_id, channel_type)`.

## Known BM-03 gaps against approved roadmap v1.3

These are gaps/divergences, not permission for Codex to redesign the feature by itself.

### Missing behavior

- Explicit consent at link-token creation is not implemented.
- Consent timestamp/version is not persisted.
- Private-chat enforcement is not implemented.
  - Telegram request DTO already receives `chat.type`.
  - `TelegramUpdateMapper` does not propagate `chat.type` into `IncomingMessage`.
  - linking therefore cannot currently enforce `chat.type == private`.
- Link status/list endpoint is not implemented.
- Link revocation endpoint is not implemented.
- Revoked-link history/state is not modeled.
- Identity resolver port (`ChannelType + external user + external conversation -> Optional<UUID>`) is not implemented.
- Active-link consistency using both external user and external conversation is not implemented.
- Database uniqueness/consistency for external conversation is not present.
- Re-linking after revocation cannot be supported because revocation is not modeled.
- BM-03-specific unit/integration/persistence/concurrency tests are not present in the current test tree.
- README still documents BM-01 and BM-02 as current scope and does not document BM-03.
- End-to-end BM-03 evidence is not represented in repository CI/artifacts.

### Explicit divergences requiring a verdict, not silent correction

1. **Token expiration**
   - Roadmap v1.3: 10 minutes.
   - Current code: 15 minutes.
   - Do not silently change either direction. Surface this when completing the DoD.

2. **Creation endpoint shape**
   - Roadmap contract: `POST /users/me/channel-links/telegram/challenges` with explicit consent.
   - Current code: `POST /users/me/link/channel/{channelType}` without consent payload.
   - Treat current endpoint as implemented reality; changing the public contract requires an explicit decision.

3. **Persistence naming/model**
   - Roadmap concept names: `channel_link_challenges`, `channel_links` with consent/revocation/history.
   - Current code: `channel_link_tokens`, `channel_accounts` with a simpler model.
   - Do not rename tables merely to match old wording. Evaluate required behavior first and preserve migration safety.

## Test/verification status at snapshot

Existing test tree contains the BM-01/BM-02 tests plus existing identity/todo tests, but no `ChannelAccountServiceTest` or dedicated BM-03 persistence/integration suite was found.

GitHub returned no commit status and no pull-request workflow run for HEAD `6932118`. Therefore:

**Do not say BM-03 or the latest commit is CI-verified.**

CI configuration runs from `bluememo/` with:

```bash
./mvnw --batch-mode --no-transfer-progress clean verify
```

and triggers on pushes to `main`, pull requests targeting `main`, or manual dispatch.

## Current README caveat

The branch README accurately describes BM-01/BM-02, but currently omits BM-03. For active BM-03 decisions/state, prefer this file + `ACTIVE_BM03.md` + approved roadmap/decision records over the README until it is updated as part of BM-03 DoD.
