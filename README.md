# BlueMemo API

BlueMemo is the backend for a personal conversational assistant. The current implementation includes user/task management plus the first Telegram messaging and identity capabilities.

Completed deliveries:
- **BM-01 — Telegram inbound/outbound messaging**
- **BM-02 — Telegram operational reliability**
- **BM-03 — Telegram ↔ BlueMemo identity linking**

BM-03 links a Telegram identity to an existing JWT-authenticated BlueMemo user. It does **not** authorize Tools or provider actions; authorization/execution context belongs to the next roadmap stage.

## Tech stack

- Java 17
- Spring Boot 4.1.0
- Spring Web MVC + `RestClient`
- Spring Security + JWT
- Spring Data JPA / Hibernate
- PostgreSQL 17
- Flyway
- JUnit / Mockito / MockMvc / MockWebServer
- PostgreSQL Testcontainers
- JaCoCo
- Docker / Docker Compose
- GitHub Actions CI

## Current capabilities

BlueMemo provides:
- user registration/login;
- BCrypt password hashing;
- stateless JWT authentication;
- profile retrieval, update and deletion;
- per-user Todo CRUD;
- Telegram inbound/outbound messaging;
- persistent inbound-event idempotency;
- Unicode-safe Telegram outbound fragmentation;
- Telegram ↔ BlueMemo link generation, verification, inspection, revocation and relinking;
- active Telegram identity resolution through channel + external user + conversation.

## Telegram identity linking

### REST endpoints

| Method | Path | Authentication | Behavior |
| --- | --- | --- | --- |
| `POST` | `/users/me/link/channel/TELEGRAM` | JWT | Requires `{"consent":true}` and returns `linkUrl` + `expirationDate` |
| `GET` | `/users/me/link/channel/TELEGRAM` | JWT | Returns the authenticated user's link history, newest first |
| `DELETE` | `/users/me/unlink/channel/TELEGRAM` | JWT | Revokes the active Telegram link, preserves history and invalidates pending link tokens |

A generated link token:
- contains 256 random bits from `SecureRandom`;
- expires after 10 minutes;
- is one-time;
- is persisted only as a SHA-256 hash;
- stores consent metadata (`consentedAt`, `consentVersion=1`).

Only a private Telegram chat can complete `/start <token>`. Telegram external user and conversation are stored separately. A valid token is consumed before final ownership/conflict checks; if a conflict prevents creation, that token remains consumed by design.

Revocation preserves the historical `channel_accounts` row and sets `revokedAt`. A revoked association stops resolving immediately. Relinking requires a fresh verified token and creates a new active history row.

`ChannelIdentityResolver` resolves a BlueMemo user only when channel, external user and conversation match an active association.

### Telegram commands

Telegram-specific command syntax is interpreted inside the Telegram adapter and converted to channel-independent `IncomingAction` values before shared processing.

| Telegram input | Generic action | Behavior |
| --- | --- | --- |
| regular text | `MESSAGE` | Replies `Recibí <text>` |
| `/start` | `WELCOME` | Replies `Bienvenido, soy un bot` |
| `/start <token>` | `LINK_CHANNEL` | Attempts channel linking |
| `/check-link` | `CHECK_CHANNEL_LINK` | Reports linked/unlinked state in private chat without exposing the BlueMemo UUID |
| `/help` | `HELP` | Returns the current help response |
| unknown slash command | `UNKNOWN_COMMAND` | Replies `Comando no reconocido` |

`TelegramCommands` / `TelegramActionConverter` own Telegram command parsing. `ProcessIncomingMessageService` consumes generic `IncomingAction` values and does not parse Telegram command strings.

### Consent and audit

Consent version `1` means consent to associate the Telegram identity/conversation with the BlueMemo account for identity resolution. It is not permission to execute Tools.

Clients must explicitly obtain the user's consent before sending `consent:true`. The API flag must represent an actual user choice and must not be asserted automatically.

Audit/history fields include:
- `linkedAt`;
- `revokedAt`;
- `consentedAt`;
- `consentVersion`.

Deleting a BlueMemo account is distinct from unlinking a channel: full account deletion removes channel associations/history, link tokens, todos and finally the user in one transaction.

## Telegram messaging architecture

```mermaid
flowchart TD
    Telegram["Telegram Bot API"] --> Controller["TelegramWebhookController"]
    Controller --> UpdateProcess["TelegramUpdateProcess"]
    UpdateProcess --> EventRepository["IncomingEventRepository"]
    UpdateProcess --> Mapper["TelegramUpdateMapper"]
    Mapper --> Converter["TelegramActionConverter"]
    Converter --> UseCase["ProcessIncomingMessageUseCase"]
    UseCase --> Router["SendMessageRouter"]
    Router --> Sender["TelegramMessageSender"]
    Sender --> Splitter["TelegramMessageSplitter"]
    Splitter --> Client["TelegramApiClient"]
    Client --> Telegram
```

| Component | Responsibility |
| --- | --- |
| `TelegramWebhookController` | Validates the Telegram webhook secret and acknowledges requests |
| `TelegramUpdateProcess` | Rejects unsupported updates, registers events and prevents duplicate processing |
| `TelegramUpdateMapper` | Maps Telegram DTOs into channel-independent messages/events |
| `TelegramActionConverter` | Converts Telegram command syntax into generic `IncomingAction` |
| `ProcessIncomingMessageService` | Executes generic action behavior and manages event status |
| `SendMessageRouter` | Selects a sender by `ChannelType` |
| `TelegramMessageSender` | Splits and sends ordered Telegram responses |
| `TelegramMessageSplitter` | Enforces Telegram's 4096-code-point limit safely |
| `TelegramApiClient` | Calls Telegram Bot API through `RestClient` |

## Telegram webhook

```http
POST /webhooks/telegram
X-Telegram-Bot-Api-Secret-Token: <secret>
Content-Type: application/json
```

The route is public in Spring Security but authenticates Telegram through the secret header; it does not require a BlueMemo JWT.

A supported update requires:
- `update_id`;
- `message.message_id`;
- `message.date`;
- `message.chat.id`;
- `message.from.id`.

Updates without `message` or required identifiers are acknowledged with `200 OK` and no outbound request. `message.text` may be absent; the current fallback is `De momento solo proceso texto`.

## Incoming-event idempotency and lifecycle

Supported Telegram updates are registered in PostgreSQL before processing. The idempotency key is:

```text
(channel_type, external_event_id)
```

For Telegram, `external_event_id` is `update_id`. Inserts use PostgreSQL `ON CONFLICT DO NOTHING`, so duplicate updates are acknowledged but not processed or answered twice.

Normal lifecycle:

```text
RECEIVED → PROCESSING → PROCESSED → ANSWERED
```

`FAILED` represents a runtime failure during reply generation/processing or outbound delivery. If writing `FAILED` also fails, the original processing exception remains the primary cause and the status-write failure is retained as a suppressed exception.

The idempotency record survives application restarts and works across instances sharing the same PostgreSQL database. It is not a distributed exactly-once guarantee across PostgreSQL and Telegram.

The defined retention policy for `incoming_events` is 30 days from `received_at`. Automatic scheduled cleanup is not yet implemented.

## Telegram outbound fragmentation

Telegram text is limited to 4096 Unicode code points per message. This constraint remains inside the Telegram adapter.

The splitter:
- uses Unicode code-point-aware operations (`codePointCount`, `offsetByCodePoints`);
- does not split UTF-16 surrogate pairs;
- prefers nearby line-break/space boundaries within 50 code points of the limit;
- otherwise performs a hard split at 4096 code points;
- preserves complete original content and fragment order;
- uses the same `chat_id` for all fragments;
- stops after the first failed fragment.

Previously accepted fragments cannot be rolled back and individual fragments are not automatically retried.

## Flyway migrations

Flyway applies pending migrations before Hibernate schema validation.

Relevant messaging/linking migrations:
- **V4** — creates channel-link token/account tables;
- **V5** — adds consent/revocation fields, active-only unique indexes and invalidates unused pre-consent tokens.

V4 was not rewritten after V5 was introduced. Existing associations are preserved without fabricating historical consent. Ambiguous pre-existing active conversations cause V5 to fail instead of silently reassigning/deleting ownership.

Do not edit already shared/applied migrations; add a new migration.

## Environment variables

| Variable | Required | Default | Description |
| --- | --- | --- | --- |
| `JWT_SECRET` | Yes | None | Base64-encoded JWT signing secret |
| `JWT_EXPIRATION_MS` | No | `900000` | JWT lifetime in milliseconds |
| `TELEGRAM_BOT_TOKEN` | Yes | None | Bot token issued by BotFather |
| `TELEGRAM_WEBHOOK_SECRET` | Yes | None | Telegram webhook secret header value |
| `TELEGRAM_BOT_NAME` | Yes | None | Bot username used to generate deep links, e.g. `BlueMemoAppDevBot` |
| `SERVER_PORT` | No | `8080` | Internal application port |
| `SPRING_DATASOURCE_URL` | QA/Prod | Local PostgreSQL URL | JDBC datasource URL |
| `SPRING_DATASOURCE_USERNAME` | QA/Prod | `app_user` locally | Database username |
| `SPRING_DATASOURCE_PASSWORD` | QA/Prod | `user` locally | Database password |
| `FLYWAY_DATABASE_URL` | QA/Prod | None | Optional Flyway-specific JDBC URL |
| `FLYWAY_DATABASE_USERNAME` | No | Datasource username | Optional Flyway-specific username |
| `FLYWAY_DATABASE_PASSWORD` | No | Datasource password | Optional Flyway-specific password |
| `DB_MAX_POOL_SIZE` | No | `10` | Production Hikari max pool size |
| `DB_MIN_IDLE` | No | `2` | Production Hikari minimum idle connections |

Never commit real credentials, JWTs, Telegram bot tokens, webhook secrets, link tokens or token hashes.

### Docker Compose `.env`

Create `bluememo/.env`:

```dotenv
POSTGRES_DB=bluememo_db
POSTGRES_USER=app_user
POSTGRES_PASSWORD=replace_with_a_strong_password
JWT_SECRET=replace_with_a_base64_encoded_secret
TELEGRAM_BOT_TOKEN=replace_with_the_development_bot_token
TELEGRAM_WEBHOOK_SECRET=replace_with_the_webhook_secret
TELEGRAM_BOT_NAME=BlueMemoAppDevBot
```

The `.env` file is ignored by Git.

## Spring profiles

| Profile | Database | Purpose |
| --- | --- | --- |
| `local` | PostgreSQL local/default overrides | Local development / Docker Compose |
| `qa` | PostgreSQL via environment | QA |
| `prod` | PostgreSQL via environment | Production |
| `test` | PostgreSQL Testcontainer | Automated integration tests |

## Running with Docker Compose

From the repository root:

```bash
cd bluememo
docker compose up --build -d
```

Docker exposes:
- API: `http://localhost:8000`
- Swagger UI: `http://localhost:8000/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8000/v3/api-docs`
- health: `http://localhost:8000/actuator/health`

Useful commands:

```bash
docker compose logs -f api
docker compose down
```

`docker compose down -v` also deletes the local PostgreSQL volume.

## Running directly

### Windows PowerShell

```powershell
cd bluememo

$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/bluememo_db"
$env:SPRING_DATASOURCE_USERNAME = "app_user"
$env:SPRING_DATASOURCE_PASSWORD = "replace_with_a_strong_password"
$env:JWT_SECRET = "replace_with_a_base64_encoded_secret"
$env:TELEGRAM_BOT_TOKEN = "replace_with_the_development_bot_token"
$env:TELEGRAM_WEBHOOK_SECRET = "replace_with_the_webhook_secret"
$env:TELEGRAM_BOT_NAME = "BlueMemoAppDevBot"

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

### Linux/macOS

```bash
cd bluememo

export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bluememo_db
export SPRING_DATASOURCE_USERNAME=app_user
export SPRING_DATASOURCE_PASSWORD=replace_with_a_strong_password
export JWT_SECRET=replace_with_a_base64_encoded_secret
export TELEGRAM_BOT_TOKEN=replace_with_the_development_bot_token
export TELEGRAM_WEBHOOK_SECRET=replace_with_the_webhook_secret
export TELEGRAM_BOT_NAME=BlueMemoAppDevBot

./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Direct execution exposes port `8080`; Docker Compose maps the API to host port `8000`.

## Exposing/registering the development Telegram webhook

For quick local development with Docker Compose:

```bash
cloudflared tunnel --url http://localhost:8000
```

For direct Maven execution, expose `http://localhost:8080` instead.

Linux/macOS webhook registration:

```bash
curl -X POST "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/setWebhook" \
  -H "Content-Type: application/json" \
  -d "{\"url\":\"https://YOUR_TUNNEL_URL/webhooks/telegram\",\"secret_token\":\"${TELEGRAM_WEBHOOK_SECRET}\",\"allowed_updates\":[\"message\"]}"
```

Windows PowerShell:

```powershell
$body = @{
    url = "https://YOUR_TUNNEL_URL/webhooks/telegram"
    secret_token = $env:TELEGRAM_WEBHOOK_SECRET
    allowed_updates = @("message")
} | ConvertTo-Json

curl.exe -X POST `
  "https://api.telegram.org/bot$env:TELEGRAM_BOT_TOKEN/setWebhook" `
  -H "Content-Type: application/json" `
  -d $body
```

## Main REST endpoints

| Method | Endpoint | Authentication | Description |
| --- | --- | --- | --- |
| `POST` | `/webhooks/telegram` | Telegram secret | Receives Telegram updates |
| `POST` | `/auth/register` | Public | Registers a BlueMemo user |
| `POST` | `/auth/login` | Public | Authenticates and returns JWT |
| `GET` | `/users/me` | JWT | Retrieves authenticated profile |
| `PATCH` | `/users/me` | JWT | Partially updates profile |
| `DELETE` | `/users/me` | JWT | Deletes user plus dependent todos/link tokens/channel history transactionally |
| `POST` | `/users/me/link/channel/TELEGRAM` | JWT | Generates Telegram link token/deep link with explicit consent |
| `GET` | `/users/me/link/channel/TELEGRAM` | JWT | Returns own Telegram association history |
| `DELETE` | `/users/me/unlink/channel/TELEGRAM` | JWT | Revokes own active Telegram association |
| `POST` | `/todos` | JWT | Creates Todo |
| `GET` | `/todos` | JWT | Lists Todos |
| `GET` | `/todos/{todoId}` | JWT | Retrieves owned Todo |
| `PUT` | `/todos/{todoId}` | JWT | Updates Todo content |
| `PATCH` | `/todos/{todoId}?status={status}` | JWT | Updates Todo status |
| `DELETE` | `/todos/{todoId}` | JWT | Deletes owned Todo |

Canonical Todo statuses are `PENDING`, `IN_PROGRESS`, and `DONE`; `COMPLETED` is accepted as an alias for `DONE`.

## Testing

Run from `bluememo/`. Docker must be running because persistence/integration tests use PostgreSQL Testcontainers.

Windows:

```powershell
.\mvnw.cmd clean verify
```

Linux/macOS:

```bash
./mvnw clean verify
```

BM-03 includes automated coverage for link generation/consumption, consent, revocation/relinking, command conversion, concurrency, account cleanup, application-context recreation and V4→V5 migration behavior. The real development Telegram bot flow was also validated end-to-end, including revocation, pending-token invalidation, relinking and persistence after application restart.

GitHub Actions runs:

```bash
./mvnw --batch-mode --no-transfer-progress clean verify
```

for pull requests targeting `main`, pushes to `main` and manual runs.

The JaCoCo report is generated at:

```text
bluememo/target/site/jacoco/index.html
```

## Project documentation

- `README.md` — implemented behavior, setup and public project documentation.
- `AGENTS.md` — stable instructions for Codex/agent work.
- `docs/codex/DECISIONS.md` — approved architectural/product invariants.
- `docs/codex/ROADMAP.md` — delivery sequence, gates and stable BM status.

Volatile operational snapshots (current HEAD, open PR state, current CI run, uncommitted work and active-delivery scratch notes) are intentionally not versioned. Inspect Git/GitHub directly for current repository state.
