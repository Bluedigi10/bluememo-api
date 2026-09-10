# BlueMemo — Roadmap

Based on the latest approved roadmap for the project. This file records delivery order and stable status; it is not a live PR/branch/CI snapshot.

## Status

| ID | Delivery | Status |
| --- | --- | --- |
| BM-00 | Audit existing `bluememo-api` base | ✅ Closed |
| BM-01 | Telegram inbound/outbound functional | ✅ Closed |
| BM-02 | Telegram operational reliability | ✅ Closed |
| BM-03 | Telegram ↔ BlueMemo user linking | ✅ Closed |
| BM-04 | Tools architecture + execution context | 🟠 Active |
| BM-05–BM-22 | Later roadmap | ⚪ Pending |

BM-03 is closed after implementation, automated verification, real Telegram DEV E2E, revocation/relinking, migration/restart validation, review resolution, PR #10 merge and successful CI on `main`.

BM-04 started on September 10, 2026. Its working branch is `feat/BM-04-tools-execution-context`. The delivery must establish trusted identity, authorization, idempotency for effectful Tool actions, and normalized Tool errors before Calendar/Tasks writes are enabled.

## Delivery sequence

| ID | Delivery | Expected outcome |
| --- | --- | --- |
| BM-04 | Tools architecture + execution context | Trusted identity, authorization, idempotency key, normalized tool errors. Closes Gate B. |
| BM-05 | Google OAuth | Encrypted tokens, minimum scopes, refresh/revocation tied to BlueMemo user. |
| BM-06 | Google Calendar read | Read calendars/events with pagination/time zones. |
| BM-07 | Google Calendar write | Create/update/cancel events with ownership/idempotency. |
| BM-08 | Google Tasks read/write | Manage tasks through a contract independent from the legacy local Todo model. |
| BM-09 | Tasks ↔ Calendar policy | Source of truth, external IDs, sync, retry/conflict policy. Closes Gate C. |
| BM-10 | Deterministic intents/commands | Known commands work without unnecessary LLM dependency. |
| BM-11 | LLM Gateway | Provider abstraction, timeouts, token/budget limits, safe fallbacks. |
| BM-12 | Conversational context | User/channel isolation, TTL, no secrets. |
| BM-13 | Hybrid router | Deterministic commands vs LLM vs Tools without letting the model impersonate identity/authorization. |
| BM-14 | Spotify OAuth | Minimum scopes, refresh, revocation. |
| BM-15 | Spotify read | Playback/library/search reads. |
| BM-16 | Spotify control | Authorized actions against a valid device. |
| BM-17 | WhatsApp inbound | Add authenticated WhatsApp adapter on proven channel/core abstractions. |
| BM-18 | WhatsApp outbound/routing | Reply through WhatsApp without rewriting central conversation logic. |
| BM-19 | Cross-cutting Tools security | Permission/ownership/AI arguments/session/token audit. |
| BM-20 | Observability | Command/provider/channel latency, errors, retry/dedup/model usage metrics. |
| BM-21 | MVP hardening | Quality gates, static/dependency analysis, concurrency/limits/revocation/migration strategy. |
| BM-22 | MVP release | End-to-end validation and operational/security release checklist. |

## Gates

### Gate A — Telegram functional
Closed by BM-01.

### Gate A.1 — Telegram robust delivery
Closed by BM-02.

### Telegram identity foundation
Completed by BM-03. Telegram can now resolve to a verified/revocable BlueMemo identity, but this does not authorize personal Tool actions.

### Gate B — Authorized personal actions
Open. BM-04 is active and must establish authorization/execution context before personal Tool writes.

### Gate C — Integration/synchronization
Must be closed by BM-09.

### Gate D — MVP release
Validated across BM-19 through BM-22.

## Planning policy

- Closed BMs stay closed; regressions become defects unless explicitly reopened.
- A new finding belongs in the functional delivery that requires it.
- Do not pull future architecture into the current BM solely because it may be useful later.
- Calendar/Tasks writes remain blocked until BM-04 closes Gate B.
- The roadmap can be split into additional small deliveries when explicitly approved and when that improves verifiability.
- Do not mark a delivery Active merely because it is next; activation requires explicit user intent to start it.
