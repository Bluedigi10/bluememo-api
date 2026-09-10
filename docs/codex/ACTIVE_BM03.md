# BM-03 — Telegram ↔ BlueMemo User Linking

Status: **ACTIVE / IMPLEMENTED AND VERIFIED; PR MERGE PENDING**

Read PROJECT_STATE.md for the current implementation, verification anchors and PR closure status; read DECISIONS.md for approved authority/invariants. BM-01/BM-02 remain closed. BM-03 does not authorize Tools or introduce BM-04+ concerns.

## Approved contract

- Keep `POST /users/me/link/channel/{channelType}`, require JWT and explicit consent.
- Expiry is 10 minutes. The previous 15-minute discrepancy is obsolete.
- Use link-token terminology and existing table names. No terminology-only migration changes.
- Consume a valid token before evaluating ownership conflicts; a rejected conflicting attempt must leave it consumed.
- Retain revoked associations with `revokedAt` and restrict uniqueness to active rows through a new migration.
- Unlink only the selected channel, preserve history, invalidate pending tokens and require a fresh verified flow.
- Full user deletion explicitly removes all dependent data/history in one transaction, following the Todo cleanup pattern.
- Keep Telegram-specific command syntax inside the Telegram adapter; shared message processing receives channel-independent `IncomingAction` values.

## Current implementation checklist

| Requirement | Current implementation |
| --- | --- |
| Authenticated creation and explicit consent | JWT principal + required `consent=true`; server consent version `1` |
| Random one-time hash-only link token | 256-bit `SecureRandom`, SHA-256, 10-minute expiry, conditional SQL consumption |
| Private-chat completion | Telegram maps `chat.type`; link action checks `privateConversation` |
| Telegram protocol isolation | `TelegramCommands` + `TelegramActionConverter` map Telegram syntax to generic `IncomingAction` |
| Ownership and conversation consistency | Active-only PostgreSQL unique indexes; external user/chat stored separately |
| Conflict consumes token | `ON CONFLICT DO NOTHING` avoids ownership-violation rollback |
| Inspect own links | `GET /users/me/link/channel/{channelType}` |
| Revoke and retain history | `DELETE /users/me/unlink/channel/{channelType}`; `revokedAt`, pending-token invalidation |
| Check active link from Telegram | `/check-link`, private-chat-only, no BlueMemo UUID exposure |
| Resolve active identity | `ChannelIdentityResolver` requires channel + external user + conversation |
| Audit | `linkedAt`, `revokedAt`, `consentedAt`, `consentVersion`; no invented legacy consent |
| Delete account | Explicit dependent cleanup and user deletion in one transaction |
| Processing failure lifecycle | Reply-generation and outbound runtime failures attempt to move the event to `FAILED` |
| Migration safety | V5 added; V4 unchanged; legacy pending tokens invalidated |
| Automated tests | 123-test local `clean verify`; PostgreSQL/Testcontainers + MockMvc + migration/restart coverage |
| Real DEV bot E2E | Verified manually through linking, status, revocation, relink and persistence after restart |
| CI | GitHub Actions run #30 succeeded for functional anchor `745a980a` |

## Verification status

BM-03 functionality and acceptance behavior have been verified through automated tests and real Telegram DEV E2E. The current code includes the PR-review fixes for Telegram command isolation, processing-failure lifecycle and required Telegram bot-name documentation.

Because documentation updates themselves create newer commits, the recorded successful CI SHA must be treated as a verification anchor rather than a permanent live HEAD. Before merge, query PR #10 directly and confirm CI is green for its current HEAD and that no new unresolved review findings exist.

## Approved minimum acceptance matrix

The following cases are covered by the current implementation/test evidence unless otherwise noted in PROJECT_STATE.md:

- unauthenticated link creation rejected;
- explicit `consent=true` required;
- sufficient token entropy and hash-only persistence;
- 10-minute expiry;
- valid `/start <token>` from private chat creates the intended link;
- invalid, expired, already-used and replaced tokens create no link;
- two concurrent consumptions create at most one link;
- valid token remains consumed when final ownership conflict is detected;
- one Telegram external user cannot actively map to two BlueMemo users;
- one BlueMemo user cannot have two active Telegram links;
- external user and conversation are stored separately;
- username changes/absence do not define ownership;
- non-private chat cannot complete linking;
- Telegram command syntax is converted in the Telegram adapter before shared processing;
- resolver requires a consistent active link and returns the expected `userId`;
- `/check-link` reports only active status and does not expose the BlueMemo UUID;
- user can inspect only own link history;
- user can revoke only own link;
- revoked link no longer resolves;
- pending tokens are invalidated on unlink;
- fresh verified linking after revocation works and preserves prior history;
- restart/context recreation does not lose link-token/link state;
- V4→V5 migration preserves representative legacy data and rejects ambiguous active conversations;
- user deletion obeys cleanup/rollback behavior;
- processing and outbound runtime failures are represented through the `FAILED` lifecycle;
- logs/responses do not expose raw token/hash/JWT/secrets.

## Out of scope for BM-03

Do not add as part of BM-03:
- Tool execution/authorization;
- Google OAuth;
- Calendar/Tasks reads or writes;
- login via Telegram;
- group/supergroup/channel linking;
- administration of other users' links;
- permanent recovery codes;
- silent reassignment of Telegram identity;
- AI/intents/conversational memory;
- Spotify;
- WhatsApp.

## Closure rule

BM-03 remains active only because PR #10 has not yet been merged. Once the current PR HEAD has green CI, no blocking review findings remain, and PR #10 is merged to `main`, mark BM-03 **Closed** and make BM-04 the active delivery.
