# BlueMemo — Approved Decisions and Invariants

This file records decisions Codex must preserve unless the user explicitly changes them.

## Decision protocol

Any proposed change to scope, architecture, acceptance criteria, priority, or technology remains a **proposal** until the user gives the final verdict.

When identifying an improvement, Codex may explain:
- what would change;
- why it may help;
- impact on the active BM and roadmap;
- alternatives;
- risks/trade-offs;
- recommendation.

Codex must not silently rewrite the roadmap or treat its recommendation as approved.

## Product/roadmap decisions

1. Telegram is the first messaging channel for the MVP; WhatsApp comes later as a second channel.
2. BM-01 and BM-02 are closed and stay closed.
3. BM-03 is identity linking only. It does not authorize Tools or personal actions.
4. BM-04 is responsible for trusted execution context/authorization and must close Gate B before Calendar/Tasks writes.
5. Google Calendar/Google Tasks come before the generative-AI layer.
6. WhatsApp adapters are intentionally deferred until after the Telegram/core path is validated.
7. New findings belong in the functional delivery that needs them; do not dump unrelated work into a generic hardening milestone.
8. The roadmap may contain more than 20 BMs if smaller 2–3-day verifiable deliveries improve control.

## Architecture decisions

### Existing API is the base
Do not rewrite BlueMemo. Reuse and evolve the existing identity/security/persistence/messaging foundation incrementally.

### Ports/adapters direction
Separate:
- external channel protocol;
- generic message/application behavior;
- external providers/tools.

Telegram must remain an adapter. Future WhatsApp must be addable without rebuilding the core message flow.

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
PostgreSQL remains BlueMemo's own persistence for users, external-account links, consent, integration state, idempotency, auditability, and future context metadata.

Prefer database uniqueness/atomic operations for cross-instance correctness.

### Time
Use `Instant` for persisted/auditable timestamps unless another representation is required at a boundary.

## BM-03 link-token security decisions

- Link creation begins from a JWT-authenticated BlueMemo user.
- Link tokens are cryptographically random and one-time.
- Persist only the token hash, never the raw token.
- Do not log raw tokens or hashes.
- Telegram username/display name/phone are not proof of account ownership.
- Telegram external user id and chat/conversation id are distinct values.
- Duplicate ownership must produce controlled conflicts; never silently reassign a Telegram identity to another BlueMemo user.

### Intentional token-consumption semantic

This is an explicit project decision:

```text
valid link token
    -> atomically consume token
    -> evaluate current link ownership/conflict state
    -> if already linked/conflicting, return controlled message
    -> token remains consumed
```

Therefore a token that reached the linking attempt is **not reusable** merely because final account creation was rejected as already linked/conflicting.

Expected invariant/test shape:

```text
given valid token
and link becomes/already is conflicting
when /start <token> reaches link attempt
then controlled conflict/already-linked response
and used_at is not null
```

Do not move link-conflict checks before token consumption if that would make the same already-processed token reusable, unless the user explicitly changes this semantic.

Note: current `generateLink` already prevents issuing a fresh token when the BlueMemo user/channel is linked. The consume-before-conflict rule still matters for races and conflicts that arise after token issuance.

## BM-02 invariants that BM-03 must preserve

- Deduplicate Telegram updates persistently using PostgreSQL, not memory.
- Duplicate `update_id` must not cause duplicate outbound replies/actions.
- Do not move Telegram's 4096-code-point limit into generic message domain code.
- Splitting must remain Unicode code-point safe.
- Fragments remain ordered and target the same chat.
- Failed outbound delivery must retain the agreed failure/status behavior.

## Naming note

Historical roadmap text uses **challenge** for the link flow. Current implementation uses `ChannelLinkToken` / `channel_link_tokens`.

When talking about code, prefer **link token**. Do not rename implementation artifacts only for terminology consistency; behavior and migration safety matter more.
