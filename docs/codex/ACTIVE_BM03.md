# BM-03 — Telegram ↔ BlueMemo User Linking

Status: **ACTIVE / PARTIAL IMPLEMENTATION**

This is the active delivery. It must be completed before BM-04.

## Goal

Allow an authenticated BlueMemo user to link a Telegram identity in a verifiable, persistent, auditable, and revocable way so BlueMemo can later resolve a trustworthy BlueMemo `userId` from Telegram identity data.

BM-03 does **not** authorize Tools/actions. BM-04 does that.

## Intended security properties

The approved roadmap requires:
- link initiation from an authenticated BlueMemo session;
- explicit consent;
- cryptographically random one-time token;
- raw token returned only for the deep-link flow and never persisted;
- only token hash persisted;
- finite expiration;
- atomic consumption;
- completion only from Telegram private chat;
- no reliance on Telegram username/display name;
- separate external user and external conversation ids;
- controlled duplicate/conflict behavior;
- ability to inspect own link status and revoke own link;
- revocation immediately disables identity resolution;
- re-linking after revocation through a new verified flow;
- resolver port independent of Telegram infrastructure.

## Current implemented flow

### Generate link

```text
JWT-authenticated BlueMemo user
  -> POST /users/me/link/channel/{channelType}
  -> ChannelAccountController
  -> ChannelAccountService.generateLink
  -> reject 409 if user/channel already linked
  -> generate 32-byte SecureRandom token
  -> SHA-256 hash
  -> upsert channel_link_tokens
  -> return https://t.me/<configured-bot>?start=<raw-token> + expiresAt
```

Current expiration in code: **15 minutes**.
Approved roadmap v1.3 says **10 minutes**.
This is an unresolved divergence; do not silently choose one.

### Complete link from Telegram

```text
Telegram sends /start <token>
  -> Telegram webhook/update process
  -> TelegramUpdateMapper
  -> IncomingMessage
  -> ProcessIncomingMessageService
  -> ChannelAccountService.linkAccount
  -> hash token
  -> validate token state/expiration/channel
  -> atomically mark used
  -> check ownership conflicts
  -> save channel_accounts
  -> reply through normal Telegram outbound flow
```

### Important intentional ordering

The token is consumed before the final linked-account conflict checks. That is intentional and must be preserved unless the user explicitly changes it.

## Current data model

### `channel_link_tokens`

```text
id UUID PK
user_id UUID FK users(id)
channel_type VARCHAR(30)
token_hash VARCHAR(255) UNIQUE
expires_at TIMESTAMPTZ
used_at TIMESTAMPTZ NULL
UNIQUE(user_id, channel_type)
```

The repository upserts on `(user_id, channel_type)`, replacing hash/expiration and clearing `used_at` when a new token is generated for an unlinked user/channel.

### `channel_accounts`

```text
id UUID PK
user_id UUID FK users(id)
channel_type VARCHAR(30)
external_user_id VARCHAR(255)
external_chat_id VARCHAR(255)
linked_at TIMESTAMPTZ
UNIQUE(channel_type, user_id)
UNIQUE(external_user_id, channel_type)
```

Current model does not include revocation/status/consent fields and does not uniquely constrain external chat/conversation.

## Current command behavior

`MessageCommands`:
- `/start`
- `/help`
- `UNKNOWN`

`/start` without a token remains a normal bot welcome.
`/start <token>` enters the linking flow.

Do not use `username` as link proof even though `IncomingMessage` currently contains it.

## DoD tracking against roadmap v1.3

Legend: ✅ implemented in current branch; 🟡 partial/divergent; ❌ not implemented/verified.

| DoD item | State | Current evidence / gap |
| --- | --- | --- |
| Flyway V4 for link-token/link data | ✅ | `V4__create_linkin_tables.sql` exists, using `channel_link_tokens` + `channel_accounts`. |
| Models/ports/use case live in identity | 🟡 | Core link service/repos live in identity; `/start` adaptation currently occurs in generic `ProcessIncomingMessageService`. |
| Creation requires authenticated user | ✅ | `@AuthenticationPrincipal` supplies BlueMemo user. |
| Explicit consent required | ❌ | No consent request/field/version. |
| >=128-bit random token | ✅ | 32 random bytes = 256 bits. |
| Store hash only | ✅ | SHA-256 hash persisted; raw token used only to form returned deep link. |
| Expiration = approved value | 🟡 | Code 15 min vs roadmap 10 min. Requires verdict. |
| One-time atomic consumption | ✅ | Conditional DB update on unused+unexpired token. |
| `/start <token>` only from private chat | ❌ | `chat.type` exists in Telegram DTO but is not mapped into `IncomingMessage` or checked. |
| Persist channel + external user + external conversation separately | ✅ | `channelType`, `externalUserId`, `externalChatId`. |
| Concurrency prevents double token consumption | ✅/needs test | Atomic SQL update exists; BM-03 concurrency test absent. |
| Prevent ambiguous active link by BlueMemo user | ✅ | Unique `(channel_type,user_id)`. |
| Prevent external Telegram user from two BlueMemo users | ✅ | Unique `(external_user_id,channel_type)`. |
| Prevent ambiguous external conversation | ❌ | No corresponding unique constraint/resolver consistency. |
| Resolver port returns BlueMemo user only for valid active link | ❌ | Not implemented. |
| Authenticated user can inspect own link | ❌ | No GET link-status/list endpoint. |
| Authenticated user can revoke own link | ❌ | No DELETE/revoke endpoint. |
| Revoked link stops resolving | ❌ | No active/revoked state. |
| New verified link possible after revoke | ❌ | No revocation lifecycle. |
| Consent/verification/revocation audited with `Instant` + version | ❌ | Only `linkedAt` and token timestamps exist. |
| Secrets excluded from logs/responses after creation | 🟡 | Implementation does not intentionally log token; dedicated BM-03 leakage tests absent. |
| Controlled invalid/conflict behavior | 🟡 | Several deterministic strings + 409 on generation exist; full roadmap matrix/tests absent. |
| BM-03 unit/integration/persistence/concurrency tests | ❌ | No dedicated BM-03 test files at snapshot. |
| `clean verify` and CI green | ❌ | No workflow/status attached to snapshot HEAD. |
| README updated for BM-03 | ❌ | README currently stops at BM-02 scope. |
| End-to-end link with dev bot verified/reproducible | ❌ repository evidence | Do not infer closure from implementation alone. |

## Minimum test matrix still expected

When the user asks to finish/test BM-03, preserve at least these cases:

- link creation rejects unauthenticated request;
- explicit consent behavior once its contract is approved;
- generated token has sufficient entropy and persisted value is hash-only;
- token expires according to the approved duration;
- valid `/start <token>` from private chat creates exactly the intended link;
- invalid token creates no link;
- expired token creates no link;
- already-used token creates no link;
- two concurrent consumptions create at most one link;
- valid token consumed even when final ownership conflict/already-linked state is detected;
- one Telegram external user cannot actively map to two BlueMemo users;
- one BlueMemo user cannot have two active Telegram links;
- external user and conversation are stored separately;
- username changes/absence do not affect identity linking;
- non-private chat cannot complete linking;
- resolver requires a consistent active link and returns the expected `userId`;
- user can inspect only own link state;
- user can revoke only own link;
- revoked link no longer resolves;
- a new verified link after revocation works;
- restart/context recreation does not lose link-token/link state;
- user deletion obeys the agreed FK/history behavior;
- logs/responses never expose raw token/hash/JWT/secrets.

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

## Before declaring BM-03 done

Codex must verify the implementation against the approved DoD, not merely against current code. Any unresolved design divergence (especially 10 vs 15 minute expiry, endpoint/consent contract, and persistence/revocation model) must be surfaced to the user rather than silently normalized.
