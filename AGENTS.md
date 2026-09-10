# BlueMemo — Codex Working Instructions

This file is the primary working context for Codex in `Bluedigi10/bluememo-api`.

## Repository layout

The Git repository root contains the Spring Boot application under `bluememo/`.
Run Maven commands from `bluememo/`.

Current technical baseline:
- Java 17
- Spring Boot 4.1.0
- Maven Wrapper
- PostgreSQL + JPA/Hibernate
- Flyway migrations
- JUnit/Mockito + MockMvc + MockWebServer
- PostgreSQL Testcontainers for integration tests
- Docker / Docker Compose
- GitHub Actions CI

## Read before changing code

For any non-trivial task, read:
1. `docs/codex/PROJECT_STATE.md` — current repository state, verification anchors, known gaps and PR closure status.
2. `docs/codex/DECISIONS.md` — approved architectural/product invariants.
3. `docs/codex/ACTIVE_BM03.md` when working on Telegram↔BlueMemo linking.
4. `docs/codex/ROADMAP.md` when a change may affect scope, delivery order, or future modules.

The repository itself is the source of truth for what is currently implemented. The decision docs and roadmap are the source of truth for intended/approved behavior. If implementation and an approved decision differ, DO NOT silently reconcile them: report the divergence and ask for/await the user's verdict before changing scope or architecture.

## Authority and scope

The user is the final authority for changes to:
- scope;
- architecture;
- acceptance criteria;
- priorities;
- technology choices;
- roadmap.

You may propose improvements, but do not convert a proposal into a project decision without explicit user approval.

BM-01 and BM-02 are CLOSED. Do not reopen or re-scope them. A regression is a new defect unless the user explicitly says otherwise.

The active delivery is BM-03: Telegram ↔ BlueMemo user linking.
Do not introduce BM-04+ concerns (Tools authorization, Google OAuth, Calendar/Tasks writes, LLM, Spotify, WhatsApp) into BM-03.

## Architectural invariants

- Evolve the existing API incrementally; do not rewrite it.
- Keep external channels as adapters. Telegram-specific protocol/API behavior must not leak into generic application/domain contracts unless the concept is genuinely channel-independent.
- Telegram command strings are interpreted in the Telegram adapter and converted to channel-independent `IncomingAction` values before shared message processing.
- `ChannelType` currently lives in `com.bluedigi.bluememo.common.domain` and is shared across identity and messaging.
- BlueMemo owns its user identity. Telegram is linked to a BlueMemo user; it does not replace BlueMemo authentication.
- Telegram integration uses BlueMemo's own webhook handling and Bot API wrapper (`RestClient`); do not add a Telegram SDK such as `telegrambots` unless explicitly approved.
- Telegram's 4096-code-point outbound limit belongs only to the Telegram adapter.
- Persisted timestamps should use `Instant` unless a domain requirement clearly needs another type.
- Preserve root exception causes when wrapping exceptions.
- Do not log or return JWTs, passwords, Telegram bot tokens, link tokens, token hashes, or other secrets.
- Prefer database constraints/atomic writes for concurrency and idempotency rather than in-memory guards.
- Use PostgreSQL/Testcontainers for persistence behavior; do not introduce H2 as a substitute.
- Flyway migration history is sensitive. Do not rename/rewrite an already shared/applied migration casually; inspect branch/history and user intent first.
- Avoid unrelated refactors, formatting churn, or package moves while implementing a BM.

## Telegram identity-linking terminology

The roadmap historically calls the one-time credential a "challenge". Current code names it `ChannelLinkToken` and the user prefers "token" when discussing implementation. Use **link token** for code-level discussion. Use "challenge" only when referring to the roadmap concept.

## Required work discipline

Before editing:
- inspect current branch, HEAD, working tree, relevant PR and relevant diff;
- inspect the existing implementation before proposing a fix;
- identify the affected use case and tests.

After editing:
- run the narrowest relevant tests first;
- when feasible run `./mvnw --batch-mode --no-transfer-progress clean verify` from `bluememo/`;
- run `git diff --check`;
- inspect the final diff for accidental changes;
- state exactly what changed, what was tested, and what remains unverified.

Never claim CI is green unless there is an actual successful CI run for the relevant commit/PR.

## Current state anchor

Active branch: `feat/BM-03-telegram-user-linking`

Base in `main` for the open BM-03 PR:
`ea61c45a1fa0cda23bb840dedb49d4e5f687f790` (BM-02 merged via PR #9).

Functional verification anchor before the latest documentation refresh:
`745a980a02b3fd86b593f80722af3a1a53a0b12f` (`added test and updated documentation`).

At that anchor, local verification covered 123 tests with zero failures/errors/skips and GitHub Actions CI run #30 completed successfully for that exact SHA. Real Telegram DEV E2E, revocation, pending-token invalidation, fresh relinking, retained history and application restart persistence were also verified by the user and are recorded in `docs/codex/PROJECT_STATE.md`.

Do not use a versioned documentation file as a live HEAD pointer: updating the file creates a newer commit. Before future reviews, query the branch/PR directly and use `PROJECT_STATE.md` for behavior/evidence context rather than assuming its recorded SHA is the current tip.

BM-03 remains active until PR #10 is merged. After merge, update the project docs so BM-03 is closed and BM-04 becomes the active delivery.
