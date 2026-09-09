# BM-03 — Telegram ↔ BlueMemo User Linking

Status: **ACTIVE / IMPLEMENTED CHANGES UNDER VERIFICATION**

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
| Real bot E2E / current CI | Not verified in this session |

## Acceptance evidence still needed before closure

Do not infer completion from code alone. Verify final clean verify, CI for the actual commit, a reproducible real dev-bot linking/unlinking flow, and persistence after restart. Review representative existing data for V5, especially ambiguous conversations. Tests must preserve the consume-before-conflict decision and cover isolation, invalid tokens, private chat, consent, revocation, new linking, account cleanup and rollback.

Historical documentation claiming absent revocation endpoints, missing-token HTTP 400, raw /start argument logging or a 15-minute expiry no longer describes the current working tree. Historical table names in roadmap v1.3 are terminology, not instructions to rename implementation artifacts.

## Approved minimum acceptance matrix

Preserve these acceptance cases. Some now have automated coverage; see PROJECT_STATE.md. This list is not a claim that every case is verified:

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
