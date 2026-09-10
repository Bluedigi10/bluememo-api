# BM-03 — Telegram ↔ BlueMemo User Linking

Status: **ACTIVE / CORE FUNCTIONALITY VERIFIED; PR CLOSURE PENDING**

Read PROJECT_STATE.md for the verified HEAD, current working changes and test results; read DECISIONS.md for authority and invariants. BM-01/BM-02 remain closed. BM-03 does not authorize Tools or introduce BM-04+ concerns.

## Approved contract

- Keep POST /users/me/link/channel/{channelType}, require JWT and explicit consent.
- Expiry is 10 minutes. The previous 15-minute code discrepancy is obsolete.
- Use link token terminology and existing table names. No terminology-only migration changes.
- Consume a valid token before evaluating ownership conflicts; a rejected conflicting attempt must leave it consumed.
- Retain revoked associations with revokedAt and restrict uniqueness to active rows, through a new migration.
- Unlink only the selected channel, preserve history, invalidate pending tokens and require a fresh verified flow.
- Full user deletion explicitly removes all dependent data/history in one transaction, following the Todo cleanup pattern.

## Current implementation checklist

| Requirement | Working implementation |
| --- | --- |
| Authenticated creation and explicit consent | JWT principal + required consent=true body; server consent version 1 |
| Random one-time hash-only link token | 256-bit SecureRandom, SHA-256, 10-minute expiry, conditional SQL consumption |
| Private-chat completion | Adapter maps chat.type to privateConversation; link command checks it |
| Ownership and conversation consistency | Active-only PostgreSQL unique indexes; external user/chat stored separately |
| Conflict consumes token | ON CONFLICT DO NOTHING avoids ownership-violation rollback |
| Inspect own links | GET /users/me/link/channel/{channelType} |
| Revoke and retain history | DELETE /users/me/unlink/channel/{channelType}; revokedAt, pending-token invalidation |
| Resolve active identity | ChannelIdentityResolver requires channel + external user + conversation |
| Audit | linkedAt, revokedAt, consentedAt and consentVersion; no invented legacy consent |
| Delete account | Explicit dependent cleanup and user deletion in one transaction |
| Migration safety | New V5; V4 unchanged; legacy pending tokens invalidated |
| Tests | Dedicated PostgreSQL/MockMvc BM-03 suite plus existing regression suites; see PROJECT_STATE.md for outcomes |
| Real DEV bot and application restart | Verified manually by the user; detailed sequence in PROJECT_STATE.md |
| CI | User reports PR #10 run #27 success for 0217b00; final HEAD CI still required |

## Acceptance evidence still needed before closure

The latest local clean verify passed 123 tests, including application-context recreation and V4-to-V5 migration against representative fixtures. The user also verified real DEV bot linking, status inspection, revocation, pending-token invalidation, fresh linking, retained history and persistence after application restart. Preserve this evidence; these items are no longer generic pending tasks.

Before closure, resolve or confirm resolution of the PR review findings and obtain successful CI for the final PR HEAD, including the additional tests currently uncommitted. The supplied successful CI run applies to 0217b00. This documentation update does not independently establish the current state of review comments or close BM-03. The acceptance matrix below remains unchanged.

Historical documentation claiming absent revocation endpoints, missing-token HTTP 400, raw /start argument logging or a 15-minute expiry no longer describes the current working tree. Historical table names in roadmap v1.3 are terminology, not instructions to rename implementation artifacts.

## Approved minimum acceptance matrix

Preserve these acceptance cases. Some now have automated coverage; see PROJECT_STATE.md. This list is not a claim that every case is verified:

- link creation rejects unauthenticated request;
- explicit consent under the approved consent=true contract;
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
