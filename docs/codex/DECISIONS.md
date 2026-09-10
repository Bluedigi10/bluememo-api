# BlueMemo — Approved Decisions and Invariants

This file records stable decisions that must be preserved unless the user explicitly changes them.

## Decision protocol

Any proposed change to scope, architecture, acceptance criteria, priority or technology remains a **proposal** until the user gives the final verdict.

Codex may explain what would change, why, impact, alternatives and trade-offs, but must not silently convert a recommendation into an approved project decision.

## Product / roadmap decisions

1. Telegram is the first messaging channel for the MVP; WhatsApp comes later as a second channel.
2. BM-00 through BM-03 are closed and stay closed unless the user explicitly reopens one.
3. BM-03 established Telegram ↔ BlueMemo identity linking. It does not authorize Tools or personal provider actions.
4. BM-04 is responsible for trusted execution context/authorization and must close Gate B before Calendar/Tasks writes.
5. Google Calendar/Google Tasks come before the generative-AI layer.
6. WhatsApp adapters are intentionally deferred until the Telegram/core path is proven.
7. New findings belong in the functional delivery that needs them; do not dump unrelated work into a generic hardening milestone.
8. The roadmap may contain more than 20 BMs if smaller 2–3-day verifiable deliveries improve control.

## Architecture decisions

### Existing API is the base

Do not rewrite BlueMemo. Reuse and evolve the existing identity/security/persistence/messaging foundation incrementally.

### Ports / adapters direction

Separate:
- external channel protocol;
- generic message/application behavior;
- external providers/tools.

Telegram remains an adapter. Future WhatsApp must be addable without rebuilding the core message flow.

Channel-specific commands are interpreted at the channel boundary and converted to channel-independent actions before shared processing.

### Telegram API strategy

Use:

```text
Telegram -> BlueMemo webhook -> generic application flow
BlueMemo -> own Telegram Bot API wrapper -> Telegram
```

Do not add the `telegrambots` SDK/library unless the user explicitly approves a technology change.

### Identity ownership

BlueMemo's UUID user is the canonical application identity. Telegram identity is linked to it; Telegram username/display name is not authentication and must not be trusted for ownership.

Persist external user and external conversation separately. Do not assume `from.id` and `chat.id` are interchangeable.

### Persistence

PostgreSQL remains BlueMemo's persistence for users, external-account links, consent, integration state, idempotency, auditability and future context metadata.

Prefer database uniqueness and atomic operations for cross-instance correctness.

### Time

Use `Instant` for persisted/auditable timestamps unless another representation is required at a boundary.

### Error preservation

Preserve the original/root exception when wrapping failures. If an error occurs while handling another error, do not replace the original cause; retain the secondary failure as suppressed when appropriate.

## BM-03 link-token security decisions

These decisions remain part of the completed identity-linking contract:

- Link creation begins from a JWT-authenticated BlueMemo user.
- Explicit consent is required before generating the link token.
- Link tokens are cryptographically random and one-time.
- Persist only the token hash, never the raw token.
- Do not log raw tokens or hashes.
- Telegram username/display name/phone are not proof of account ownership.
- Telegram external user id and chat/conversation id are distinct values.
- Duplicate ownership must produce controlled conflicts; never silently reassign a Telegram identity to another BlueMemo user.
- Linking is completed only from an allowed private conversation.
- Revoked associations remain as history and no longer resolve identity.
- Relinking after revocation requires a fresh verified link-token flow.

### Intentional token-consumption semantic

This is an explicit project decision:

```text
valid link token
    -> atomically consume token
    -> evaluate current link ownership/conflict state
    -> if already linked/conflicting, return controlled message
    -> token remains consumed
```

A token that reached the linking attempt is therefore not reusable merely because final account creation was rejected.

Do not move link-conflict checks before token consumption if that would make the same attempted token reusable, unless the user explicitly changes this semantic.

## BM-02 invariants preserved by later work

- Deduplicate Telegram updates persistently using PostgreSQL, not memory.
- Duplicate `update_id` must not cause duplicate outbound replies/actions.
- Telegram's 4096-code-point limit stays outside the generic message domain.
- Splitting remains Unicode code-point safe.
- Fragments remain ordered and target the same chat.
- Failed processing/outbound delivery retains the agreed event-failure behavior.

## Account deletion and channel unlinking — approved 2026-09-09

- Deleting a BlueMemo account permanently deletes its associated todos, link tokens, channel links and linking/revocation history.
- Dependent records are removed explicitly before deleting the user, inside the same transaction. Failure must roll back the deletion.
- Unlinking affects only the selected channel's active association. Preserve the BlueMemo account, other channels and linking/revocation history.
- A revoked association must stop resolving identity immediately.
- Pending tokens for an unlinked channel must not bypass the fresh-link requirement.
- Account deletion and channel unlinking are separate use cases; do not reuse history-erasing account cleanup as unlinking.
- Associations are retained with `revokedAt`; ownership uniqueness applies to active rows through migration-backed constraints/indexes.

## Link contract and retained history — approved 2026-09-09

- Keep `POST /users/me/link/channel/{channelType}` with explicit consent; do not replace it with the historical challenges route.
- Preserve revoked associations in `channel_accounts` with `revokedAt`.
- Restrict ownership uniqueness to active rows using a new migration; do not rewrite V4.
- Link-token expiry is 10 minutes.

## Naming note

Historical roadmap text may use **challenge** for the link flow. Current implementation uses `ChannelLinkToken` / `channel_link_tokens`.

When discussing code, prefer **link token**. Do not rename implementation artifacts only for terminology consistency.
