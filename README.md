# BlueMemo API

BlueMemo is a REST API for managing personal tasks. It includes user registration and login, JWT authentication, profile management, and a per-user task CRUD.

## Features

- User registration and login
- Password hashing with BCrypt
- Stateless JWT authentication
- Profile retrieval, partial updates, and deletion
- Task creation, retrieval, updating, filtering, sorting, and deletion
- Task isolation by authenticated user
- Pagination and status filtering
- Validation and centralized error handling
- Database migrations with Flyway
- OpenAPI documentation with Swagger UI
- Health checks with Spring Boot Actuator
- Unit and HTTP integration tests with MockMvc
- Code coverage reports with JaCoCo
- Continuous integration with GitHub Actions
- Containers for the API and PostgreSQL

## Tech Stack

- Java 17
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Security
- Spring Data JPA / Hibernate
- Flyway
- PostgreSQL 17
- JJWT 0.13.0
- Springdoc OpenAPI 3.0.3
- Maven Wrapper
- JUnit, Mockito, MockMvc, H2, and JaCoCo
- Docker and Docker Compose

## Project Structure

```text
bluememo-web/
├── .github/workflows/ci.yml
├── README.md
└── bluememo/
    ├── src/main/java/com/bluedigi/bluememo/
    │   ├── config/                 # Security, JWT filter, and OpenAPI
    │   ├── identity/               # Authentication and users
    │   ├── todo/                   # Task management
    │   └── shared/                 # JWT and error handling
    ├── src/main/resources/
    │   ├── db/migration/           # Versioned Flyway migrations
    │   ├── application.properties
    │   ├── application-local.properties
    │   ├── application-qa.properties
    │   └── application-prod.properties
    ├── src/test/                   # Unit and HTTP integration tests
    ├── compose.yaml
    ├── Dockerfile
    └── pom.xml
```

## Requirements

To run the complete stack with containers:

- Docker Desktop or Docker Engine with Docker Compose

To run the API directly:

- JDK 17
- PostgreSQL

The Maven Wrapper is included, so a separate Maven installation is not required.

## Environment Variables

### Application

| Variable | Required | Default | Description |
| --- | --- | --- | --- |
| `JWT_SECRET` | Yes | None | Base64-encoded secret used to sign JWTs |
| `JWT_EXPIRATION_MS` | No | `900000` | Token lifetime in milliseconds |
| `SERVER_PORT` | No | `8080` | Internal application port |
| `SPRING_DATASOURCE_URL` | QA/Prod | Local uses `jdbc:postgresql://localhost:5432/bluememo_db` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | QA/Prod | Local uses `app_user` | PostgreSQL username |
| `SPRING_DATASOURCE_PASSWORD` | QA/Prod | Local uses `user` | PostgreSQL password |
| `DB_MAX_POOL_SIZE` | No | `10` | Maximum Hikari pool size in production |
| `DB_MIN_IDLE` | No | `2` | Minimum idle connections in production |

`JWT_SECRET` must decode to a key of at least 32 bytes. You can generate one with:

```bash
openssl rand -base64 32
```

Do not commit real secrets or credentials to Git.

### Docker Compose

Create `bluememo/.env`:

```dotenv
POSTGRES_DB=bluememo_db
POSTGRES_USER=app_user
POSTGRES_PASSWORD=replace_with_a_strong_password
JWT_SECRET=replace_with_a_base64_encoded_secret
```

Docker Compose maps these variables to the configuration required by Spring and automatically activates the `local` profile.

## Spring Profiles

| Profile | Database | Migrations | Hibernate | Purpose |
| --- | --- | --- | --- | --- |
| `local` | PostgreSQL with local defaults or overrides | Flyway | `validate` | Local development and Docker Compose |
| `qa` | PostgreSQL configured through environment variables | Flyway | `validate` | Quality assurance |
| `prod` | PostgreSQL configured through environment variables | Flyway | `validate` | Production |
| `test` | H2 in PostgreSQL compatibility mode | Flyway | `validate` | Automated tests |

No profile is active by default. Select one when starting the application; Docker Compose uses `local`.

Flyway applies pending migrations before Hibernate validates the schema. The initial migration is located at:

```text
bluememo/src/main/resources/db/migration/V1__create_initial_schema.sql
```

Do not modify migrations that have already been applied. Add subsequent schema changes as new versions, for example, `V2__add_priority_to_todos.sql`.

If your local volume was created before Flyway was integrated and you do not need to preserve its data, recreate it with:

```bash
docker compose down -v
docker compose up --build -d
```

`down -v` permanently deletes the local PostgreSQL data.

## Running with Docker Compose

From the repository root:

```bash
cd bluememo
docker compose up --build -d
```

Available services:

- API: `http://localhost:8000`
- Swagger UI: `http://localhost:8000/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8000/v3/api-docs`
- Health check: `http://localhost:8000/actuator/health`

Useful commands:

```bash
# View API logs
docker compose logs -f api

# Stop services while preserving the database
docker compose down

# Stop services and delete local data
docker compose down -v
```

## Running Locally

Start PostgreSQL and create the database. Then configure the JWT secret and, if you will not use the local defaults, the datasource variables.

### Windows PowerShell

```powershell
cd bluememo

$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/bluememo_db"
$env:SPRING_DATASOURCE_USERNAME = "app_user"
$env:SPRING_DATASOURCE_PASSWORD = "replace_with_a_strong_password"
$env:JWT_SECRET = "replace_with_a_base64_encoded_secret"

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

### Linux or macOS

```bash
cd bluememo

export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bluememo_db
export SPRING_DATASOURCE_USERNAME=app_user
export SPRING_DATASOURCE_PASSWORD=replace_with_a_strong_password
export JWT_SECRET=replace_with_a_base64_encoded_secret

./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The API is available at `http://localhost:8080` unless `SERVER_PORT` specifies a different port.

For QA or production, select the appropriate profile and provide all required connection variables.

## Authentication

Registration and login return:

```json
{
  "token": "<JWT>"
}
```

Send the token to protected endpoints using:

```http
Authorization: Bearer <JWT>
```

By default, the token expires after 15 minutes. `/auth/**`, Swagger/OpenAPI, and `/actuator/health` are public; all other endpoints require authentication.

## Endpoints

| Method | Endpoint | Authentication | Response | Description |
| --- | --- | --- | --- | --- |
| `POST` | `/auth/register` | No | `201` | Registers a user and returns a JWT |
| `POST` | `/auth/login` | No | `200` | Authenticates the user and returns a JWT |
| `GET` | `/users/me` | Yes | `200` | Retrieves the authenticated user's profile |
| `PATCH` | `/users/me` | Yes | `200` | Partially updates the profile |
| `DELETE` | `/users/me` | Yes | `204` | Deletes the user and their tasks |
| `POST` | `/todos` | Yes | `201` | Creates a task with `PENDING` status |
| `GET` | `/todos` | Yes | `200` | Lists the user's tasks |
| `GET` | `/todos/{todoId}` | Yes | `200` | Retrieves a task owned by the user |
| `PUT` | `/todos/{todoId}` | Yes | `200` | Updates the title and description |
| `PATCH` | `/todos/{todoId}?status={status}` | Yes | `200` | Updates the status |
| `DELETE` | `/todos/{todoId}` | Yes | `204` | Deletes a task owned by the user |

### List Parameters

`GET /todos` accepts:

| Parameter | Default | Accepted values |
| --- | --- | --- |
| `status` | No filter | `PENDING`, `IN_PROGRESS`, `COMPLETED` |
| `sortBy` | `createdAt` | `createdAt`, `updatedAt`, `title`, `status` |
| `direction` | `desc` | `asc`, `desc` |
| `page` | `0` | Zero-based page number |
| `size` | `10` | Items per page |

The paginated response contains `content`, `page`, `size`, `numberOfElements`, and `totalElements`.

## Usage Examples

The examples use the Docker URL: `http://localhost:8000`.

### Register a User

```bash
curl -X POST http://localhost:8000/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "David",
    "email": "david@example.com",
    "password": "Password123!"
  }'
```

The name must contain between 2 and 100 characters, and the password must contain at least 8 characters.

### Log In

```bash
curl -X POST http://localhost:8000/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "david@example.com",
    "password": "Password123!"
  }'
```

### Create a Task

```bash
curl -X POST http://localhost:8000/todos \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Finish BlueMemo",
    "description": "Validate the API documentation"
  }'
```

`title` cannot be blank, and both fields accept up to 255 characters.

### List Tasks

```bash
curl "http://localhost:8000/todos?status=IN_PROGRESS&sortBy=updatedAt&direction=asc&page=0&size=10" \
  -H "Authorization: Bearer <JWT>"
```

### Update a Task

Both `title` and `description` must be sent to this endpoint.

```bash
curl -X PUT http://localhost:8000/todos/<TODO_ID> \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Finish and review BlueMemo",
    "description": "Verify the documented configuration"
  }'
```

### Update the Status

```bash
curl -X PATCH "http://localhost:8000/todos/<TODO_ID>?status=COMPLETED" \
  -H "Authorization: Bearer <JWT>"
```

### Update the Profile

`password` is the current password and authorizes the operation. The remaining fields are optional; `newPassword` changes the password.

```bash
curl -X PATCH http://localhost:8000/users/me \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "David Martinez",
    "phone": "5512345678",
    "birthdate": "1995-08-20",
    "password": "Password123!",
    "newPassword": "NewPassword123!"
  }'
```

### Delete the Profile

```bash
curl -X DELETE http://localhost:8000/users/me \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "password": "NewPassword123!"
  }'
```

## Errors

The API uses:

- `400 Bad Request`: invalid fields, UUIDs, pagination, sorting, or statuses
- `401 Unauthorized`: missing, invalid, or expired credentials or token
- `403 Forbidden`: access denied by Spring Security
- `404 Not Found`: user or task not found; a task owned by another user is also reported as not found
- `409 Conflict`: duplicate email, phone number, or task title
- `500 Internal Server Error`: unhandled error

Error response format:

```json
{
  "message": "Todo not found",
  "status": 404,
  "path": "/todos/00000000-0000-0000-0000-000000000000",
  "timestamp": "2026-08-10T12:00:00"
}
```

## Testing and Coverage

The test suite includes unit tests for services and JWT handling, a context test, and HTTP integration tests covering authentication, validation, security, user isolation, and the complete task lifecycle.

Run the complete verification from `bluememo/`:

### Windows PowerShell

```powershell
.\mvnw.cmd clean verify
```

### Linux or macOS

```bash
./mvnw clean verify
```

Tests use the `test` profile. Flyway creates the schema in H2 before Hibernate validates it.

The JaCoCo report is generated at:

```text
bluememo/target/site/jacoco/index.html
```

## Continuous Integration

GitHub Actions runs the following command on every push and pull request targeting `main`:

```bash
./mvnw --batch-mode --no-transfer-progress clean verify
```

The workflow retains the JAR, Surefire reports, and JaCoCo report for seven days.

## Building and Running the JAR

### Windows PowerShell

```powershell
cd bluememo
.\mvnw.cmd clean package
java -jar target/bluememo-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### Linux or macOS

```bash
cd bluememo
./mvnw clean package
java -jar target/bluememo-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

The JAR requires the same datasource and JWT variables described above.
