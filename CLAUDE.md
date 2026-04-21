# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Commands

```bash
# Start local dependencies (PostgreSQL + Adminer)
docker compose up -d

# Run the application
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.villo.truco.domain.model.match.MatchTest"

# Build (includes coverage verification at 70% minimum)
./gradlew build

# View test coverage report
# open build/reports/jacoco/test/html/index.html
```

API docs available at `http://localhost:8080/swagger-ui/index.html` when running locally.

## Architecture

This project uses **Clean / Hexagonal Architecture with DDD**, enforced at build time via **ArchUnit
** (`CleanArchitectureTest.java`). The dependency rule is strict and one-directional:

```
Domain → Application → Infrastructure
```

- **Domain** (`com.villo.truco.domain`): Pure Java, zero Spring. Contains aggregates, value objects,
  domain events, and output port interfaces.
- **Application** (`com.villo.truco.application`): Orchestrates use cases. Contains command/query
  handlers, input port interfaces, DTO assemblers, and application event handlers. No Spring
  framework imports allowed.
- **Infrastructure** (`com.villo.truco.infrastructure`): Spring Boot wiring, JPA entities, REST
  controllers, WebSocket config, security, schedulers, and repository implementations.

Two additional bounded contexts follow the same layered structure internally:

- **Auth** (`com.villo.truco.auth`): User registration, JWT login/refresh/logout.
- **Social** (`com.villo.truco.social`): Friend requests, resource invitations (match/league/cup),
  ephemeral direct messages.

### Key Architectural Rules (enforced by ArchUnit)

- HTTP controllers must depend on **input port interfaces**, never directly on use case
  implementations.
- Domain aggregates (Match, Bot, etc.) must not import from each other — cross-domain communication
  goes through domain events.
- Only `BotCard` may import from the shared cards kernel; the Bot domain otherwise has no access to
  Match domain types.

### Real-time Communication

WebSocket/STOMP is used for game updates:

- Connection: `/ws` (native) or `/ws-sockjs` (SockJS fallback)
- User queues: `/user/queue/match`, `/user/queue/league`, `/user/queue/cup`, `/user/queue/chat`,
  `/user/queue/social`
- Public topics: `/topic/public-match-lobby`, `/topic/public-league-lobby`,
  `/topic/public-cup-lobby`

## Testing

- Tests use **H2 in-memory (PostgreSQL mode)** — no Docker needed.
- Flyway is disabled in tests; DDL is `create-drop`.
- JaCoCo enforces **70% minimum line coverage** (configurable via `coverageMinimum` gradle
  property).
- Architecture tests in `CleanArchitectureTest` run as part of the test suite and will fail if
  layering is violated.

## Local Configuration

All defaults are in `src/main/resources/application.yaml`. Key env vars (all have working defaults
for local dev):

| Variable            | Default                               | Purpose         |
|---------------------|---------------------------------------|-----------------|
| `TRUCO_DB_USER`     | `truco`                               | DB username     |
| `TRUCO_DB_PASSWORD` | `truco`                               | DB password     |
| `TRUCO_JWT_SECRET`  | `truco-local-dev-secret-key-32-bytes` | JWT signing key |

Docker Compose starts PostgreSQL on port `5432` and Adminer on `8081`.
