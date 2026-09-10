# BlueMemo — Codex Working Instructions

This file contains stable working rules for Codex in `Bluedigi10/bluememo-api`.
It must not be used as a live repository-state snapshot.

## Repository layout

The Git repository root contains the Spring Boot application under `bluememo/`.
Run Maven commands from `bluememo/`.

Technical baseline:
- Java 17
- Spring Boot 4.1.0
- Maven Wrapper
- PostgreSQL + JPA/Hibernate
- Flyway migrations
- JUnit/Mockito + MockMvc + MockWebServer
- PostgreSQL Testcontainers for persistence/integration tests
- Docker / Docker Compose
- GitHub Actions CI

## Source of truth

Before any non-trivial task:
1. Inspect the current branch, HEAD, working tree, relevant PR and diff directly from Git/GitHub.
2. Read `README.md` for the implemented public behavior and local setup.
3. Read `docs/codex/DECISIONS.md` for approved architectural/product invariants.
4. Read `docs/codex/ROADMAP.md` when scope, delivery order or future modules are relevant.

Do not infer current HEAD, CI state, active branch, pending review comments or uncommitted work from versioned documentation. Those facts are volatile and must be queried directly.

Local operational notes such as `docs/codex/PROJECT_STATE.md` or `docs/codex/ACTIVE_DELIVERY.md` may exist in a developer workspace, but they are intentionally ignored by Git and are not authoritative over the repository or GitHub state.

## Authority and scope

The user is the final authority for changes to:
- scope;
- architecture;
- acceptance criteria;
- priorities;
- technology choices;
- roadmap.

A recommendation remains a proposal until the user explicitly approves it. Do not silently change roadmap scope or architecture.

Closed BMs stay closed unless the user explicitly reopens them. Regressions in a closed BM are defects, not automatic scope extensions.

Do not pull later-BM concerns into the current delivery only because they may be useful in the future.

## Architectural invariants

- Evolve the existing API incrementally; do not rewrite it.
- Keep external channels as adapters. Channel-specific protocol/API behavior must not leak into generic application/domain contracts unless the concept is genuinely channel-independent.
- Telegram command strings are interpreted in the Telegram adapter and converted to channel-independent `IncomingAction` values before shared message processing.
- `ChannelType` is shared across identity and messaging.
- BlueMemo owns the canonical user identity. External channels are linked to a BlueMemo user; they do not replace BlueMemo authentication.
- Telegram integration uses BlueMemo's own webhook handling and Bot API wrapper based on `RestClient`; do not add a Telegram SDK such as `telegrambots` without explicit approval.
- Telegram's 4096-code-point outbound limit belongs only to the Telegram adapter.
- Persisted/auditable timestamps should use `Instant` unless a boundary/domain requirement requires something else.
- Preserve root exception causes when wrapping exceptions. If failure handling also fails, retain the original failure as primary and attach secondary failures as suppressed when appropriate.
- Do not log or return JWTs, passwords, Telegram bot tokens, link tokens, token hashes or other secrets.
- Prefer database constraints and atomic writes for concurrency/idempotency instead of in-memory guards.
- Use PostgreSQL/Testcontainers for persistence behavior; do not introduce H2 as a substitute.
- Flyway history is sensitive. Do not rewrite an already shared/applied migration casually; add a new migration when schema evolution is required.
- Avoid unrelated refactors, formatting churn or package moves while implementing a BM.

## Telegram identity-linking terminology

Historical roadmap text may call the one-time linking credential a `challenge`. Current implementation uses `ChannelLinkToken` / `channel_link_tokens`.

Use **link token** when discussing implementation. Do not rename implementation artifacts only for terminology consistency.

## Required work discipline

Before editing:
- inspect the actual implementation;
- identify the affected use case and tests;
- inspect the current Git/GitHub state instead of trusting a stale snapshot.

After editing:
- run the narrowest relevant tests first;
- when feasible run `./mvnw --batch-mode --no-transfer-progress clean verify` from `bluememo/`;
- run `git diff --check`;
- inspect the final diff for accidental changes;
- state exactly what changed, what was tested and what remains unverified.

Never claim CI is green unless an actual successful run exists for the relevant commit/PR.
