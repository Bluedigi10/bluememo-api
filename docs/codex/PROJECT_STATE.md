# BlueMemo — Current Project State

## Current delivery

- Active delivery: **BM-03 — Telegram ↔ BlueMemo user linking**.
- Pull request: **#10 — `feat/bm 03 telegram user linking`**.
- PR branch: `feat/BM-03-telegram-user-linking` → `main`.
- `main` baseline for BM-03: `ea61c45a1fa0cda23bb840dedb49d4e5f687f790` (BM-02 merged via PR #9).
- BM-00, BM-01 and BM-02 remain closed.
- BM-04+ concerns remain out of scope until BM-03 is merged/closed.

### Functional verification anchor

The latest code/test commit independently reviewed before this documentation refresh is:

`745a980a02b3fd86b593f80722af3a1a53a0b12f` — `added test and updated documentation`.

At that commit:

- the branch compared **22 commits ahead and 0 behind** `ea61c45`;
- PR #10 was open, mergeable and not a draft;
- GitHub Actions **CI run #30 (`34449297227`) completed successfully** for exactly `745a980`;
- the CI `Build and test` job ran `./mvnw --batch-mode --no-transfer-progress clean verify` and succeeded.

Do **not** treat a HEAD SHA stored in a versioned documentation file as the live branch tip. Updating documentation creates a newer commit by definition. For the current PR HEAD, commit count, review state and CI, query Git/GitHub directly. The SHA above is a **functional verification anchor**, not a self-updating HEAD pointer.

## Current BM-03 implementation

- `POST /users/me/link/channel/{channelType}` requires the authenticated BlueMemo user and explicit `{"consent":true}`.
- Telegram link tokens use 256 bits from `SecureRandom`, expire after 10 minutes, are one-time, and only their SHA-256 hash is persisted.
- Consent version `1` and `consentedAt` are stored with the token and copied to the verified channel association.
- Telegram deep-link completion is private-chat-only.
- Telegram-specific command syntax is interpreted inside the Telegram adapter by `TelegramActionConverter` / `TelegramCommands` and converted to the channel-independent `IncomingAction` model before reaching shared message processing.
- `/start` maps to `WELCOME`; `/start <token>` maps to `LINK_CHANNEL`; `/check-link` maps to `CHECK_CHANNEL_LINK`; `/help` maps to `HELP`; unknown slash commands map to `UNKNOWN_COMMAND`; normal text maps to `MESSAGE`.
- `ProcessIncomingMessageService` operates on `IncomingAction`, not Telegram command strings.
- Reply-generation failures and outbound failures are both inside the processing failure lifecycle; runtime failures attempt to move the incoming event to `FAILED` before propagating `MessageException`.
- Link-token consumption remains atomic and occurs before ownership/conflict checks. A token that reaches a conflicting link attempt remains consumed by design.
- Account insertion uses `ON CONFLICT DO NOTHING` to produce controlled ownership/conversation conflict responses without rolling back consumed-token semantics.
- A PostgreSQL user-row lock coordinates link generation, linking, unlinking and account deletion for the same BlueMemo user.
- Active ownership is constrained in PostgreSQL for BlueMemo user, external user and conversation.
- `ChannelIdentityResolver` resolves a BlueMemo UUID only when channel + external user + conversation match an active association.
- `/check-link` is private-chat-only and reports linked/unlinked state without exposing the BlueMemo UUID.
- `GET /users/me/link/channel/{channelType}` returns only the authenticated user's association history ordered by `linkedAt` descending.
- `DELETE /users/me/unlink/channel/{channelType}` revokes the selected active association, preserves history and invalidates outstanding tokens for that user/channel.
- Relinking requires a fresh verified token and creates a new active history row while the revoked row remains preserved.
- Full BlueMemo account deletion explicitly removes channel associations/history, link tokens and todos before deleting the user, in one transaction.

## Persistence and migrations

- V4 creates `channel_link_tokens` and `channel_accounts`.
- V4 remains unchanged after V5 was introduced.
- V5 adds `revoked_at`, `consented_at` and `consent_version` fields.
- V5 replaces unconditional channel-account uniqueness with active-only unique indexes.
- V5 invalidates unused pre-consent link tokens instead of inventing consent for them.
- Existing associations keep null consent fields when no historical consent exists.
- Conflicting pre-existing active conversations cause migration failure rather than silent deletion or reassignment.
- Revoked associations retain link/revocation/consent history.

## Automated verification

The latest recorded local full verification for the committed BM-03 test set is:

- `123 tests`;
- `0 failures`;
- `0 errors`;
- `0 skipped`;
- PostgreSQL Testcontainers;
- `clean verify` successful.

Coverage includes:

- authenticated link generation and explicit consent;
- entropy, hash-only persistence and 10-minute expiry;
- invalid, expired, already-used and replaced tokens;
- private-chat-only linking;
- Telegram command-to-`IncomingAction` conversion;
- `/check-link` before linking, while linked, after revocation and after fresh relinking;
- sender/conversation consistency and no identity/token exposure;
- webhook idempotency;
- ownership isolation;
- active identity resolution;
- token-consumption conflict semantics;
- same-token concurrency;
- external-account and conversation conflicts;
- revocation and pending-token invalidation;
- account cleanup and rollback on cleanup failure;
- application-context recreation against persistent PostgreSQL state;
- real Flyway V4→V5 migration fixtures, including legacy-token invalidation, active-only uniqueness and rollback on ambiguous legacy conversations.

GitHub Actions CI run #30 also succeeded for functional anchor `745a980`. The workflow runs `clean verify` on pull requests targeting `main`.

## Real Telegram DEV E2E evidence

The BM-03 flow was manually verified against the real development bot `@BlueMemoAppDevBot`, the local BlueMemo API and PostgreSQL.

Verified sequence:

1. `/check-link` before linking reported that Telegram was not linked.
2. `POST /users/me/link/channel/TELEGRAM` generated a deep link and persisted a hash-only token with `used_at = NULL`, expiry and consent metadata.
3. Opening the Telegram deep link triggered `/start <token>` and returned `Cuenta vinculada con éxito. ¡Bienvenido!`.
4. Token `used_at` was populated and an active `channel_accounts` row was created with Telegram external user/chat IDs, `linked_at`, consent metadata and `revoked_at = NULL`.
5. `/check-link` then reported linked.
6. `GET /users/me/link/channel/TELEGRAM` returned the association and audit fields.
7. `DELETE /users/me/unlink/channel/TELEGRAM` returned `204`, preserved the history row and populated `revoked_at`.
8. `/check-link` immediately reported unlinked after revocation.
9. A separately generated pending token was verified with `used_at = NULL`; unlink invalidated it by populating `used_at` before it could be used.
10. A fresh token after revocation successfully relinked Telegram, creating a new active row while preserving the revoked row.
11. The history endpoint returned the new active row first and the older revoked row second.
12. The application was stopped and started again; Flyway validated schema version 5 as up to date and the active/revoked association history remained available.

This provides real E2E evidence for generation, Telegram delivery, token consumption, identity resolution, revocation, pending-token invalidation, fresh relinking, retained history and persistence across application restart.

## PR review status

The PR review findings addressed in code/documentation include:

- documenting required `TELEGRAM_BOT_NAME` configuration;
- keeping Telegram command parsing inside the Telegram adapter;
- including reply-generation/link failures in the incoming-event failure lifecycle;
- fixing the Linux/macOS bot-name environment assignment;
- refreshing stale Codex project-state documentation.

At the start of this documentation refresh, all earlier code/configuration review threads were resolved/outdated. The only unresolved thread concerned the stale `PROJECT_STATE.md` snapshot itself; this refresh addresses that issue by removing the self-invalidating live-HEAD model and using a functional verification anchor instead.

## Remaining closure work

BM-03 is functionally implemented and acceptance evidence is present. The remaining release step is procedural:

1. verify CI is green for the **current PR HEAD** after any final documentation-only commits;
2. confirm there are no new unresolved PR review threads;
3. merge PR #10 into `main`;
4. after merge, mark BM-03 closed and make BM-04 the active delivery.

No additional BM-03 feature work is currently required by the approved roadmap or acceptance evidence.
