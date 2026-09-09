# BlueMemo — Roadmap Context

Based on the latest project roadmap available when this Codex context was generated: **Roadmap v1.3 — Telegram Identity (2026-09-01)**.

This is scope context, not permission to implement future BMs early.

## Current status

| ID | Delivery | Status |
| --- | --- | --- |
| BM-00 | Audit existing `bluememo-api` base | ✅ Closed |
| BM-01 | Telegram inbound/outbound functional | ✅ Closed — PR #8 |
| BM-02 | Telegram operational reliability | ✅ Closed — PR #9 / `ea61c45` |
| BM-03 | Telegram ↔ BlueMemo user linking | 🟠 Active |
| BM-04–BM-22 | Later roadmap | ⚪ Pending |

## Delivery sequence

| ID | Delivery | Expected outcome |
| --- | --- | --- |
| BM-03 | Telegram ↔ BlueMemo user linking | Verifiable/revocable Telegram identity linked to BlueMemo user. |
| BM-04 | Tools architecture + execution context | Trusted identity, authorization, idempotency key, normalized tool errors. Closes Gate B. |
| BM-05 | Google OAuth | Encrypted tokens, minimum scopes, refresh/revocation tied to BlueMemo user. |
| BM-06 | Google Calendar read | Read calendars/events with pagination/time zones. |
| BM-07 | Google Calendar write | Create/update/cancel events with ownership/idempotency. |
| BM-08 | Google Tasks read/write | Manage tasks through a contract independent from the legacy local Todo model. |
| BM-09 | Tasks ↔ Calendar policy | Source of truth, external IDs, sync, retry/conflict policy. Closes Gate C. |
| BM-10 | Deterministic intents/commands | Known commands work without unnecessary LLM dependency. |
| BM-11 | LLM Gateway | Provider abstraction, timeouts, token/budget limits, safe fallbacks. |
| BM-12 | Conversational context | User/channel isolation, TTL, no secrets. |
| BM-13 | Hybrid router | Deterministic commands vs LLM vs Tools without letting model impersonate identity/authorization. |
| BM-14 | Spotify OAuth | Minimum scopes, refresh, revocation. |
| BM-15 | Spotify read | Playback/library/search reads. |
| BM-16 | Spotify control | Authorized actions against valid device. |
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

### Gate B — Authorized personal actions
Not closed by BM-03. BM-03 establishes trusted linking/resolution; BM-04 must establish authorization/execution context before personal Tool writes.

### Gate C — Integration/synchronization
Must be closed by BM-09.

### Gate D — MVP release
Validated across BM-19 through BM-22.

## Planning policy

- Closed BMs stay closed; regressions become defects unless explicitly re-opened.
- A new finding belongs in the functional delivery that requires it.
- Do not pull future architecture into the active BM "because it will be needed later".
- BM-03 must be completed before BM-04.
- Calendar/Tasks writes remain blocked until BM-04 closes Gate B.
- The roadmap can be split into additional small deliveries when explicitly approved and when that improves verifiability.
