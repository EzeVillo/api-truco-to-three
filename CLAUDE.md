# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Commands

```bash
./gradlew test        # Run all tests (unit, architecture, security, concurrency)
./gradlew bootRun     # Start server at http://localhost:8080
./gradlew build       # Build project
./gradlew test --tests "com.villo.truco.SomeTest"  # Run a single test class
```

Swagger UI available at `http://localhost:8080/swagger-ui.html` when running.

## Architecture

Clean Architecture + DDD Tactical. Dependency direction: `infrastructure` → `application` →
`domain`. This is enforced by ArchUnit in `CleanArchitectureTest.java`.

```
domain/       Pure business rules. No framework imports.
application/  Use case orchestration via ports. No transport/framework.
infrastructure/ HTTP, WebSocket, JWT, persistence, Spring config.
```

**If a change affects business behavior, start in `domain`.**

### Key Domain Concepts

- **Match** (aggregate root): a best-of-N game session between 2 players. Manages games score,
  current round, player states. States: `WAITING_FOR_PLAYERS` → `READY` → `IN_PROGRESS` →
  `FINISHED`.
- **Round** (entity within Match): a single game. Manages card play, Truco/Envido state machines,
  per-hand scoring.
- **League** (aggregate root): round-robin league across 2+ players.
- Domain events (30+) accumulate in aggregates, are published by use cases via `MatchEventNotifier`,
  then cleared.

### Standard Change Flow

1. Locate the impacted rule and its owning layer.
2. Apply the smallest safe change in the correct layer.
3. Side-effect order: load state → execute domain logic → persist → publish events → clear event
   buffers.
4. Add/update tests for changed behavior.
5. Validate architecture boundaries remain intact.

### Type Safety

Convert external primitives (`String`, `int`) into semantic domain types as early as possible — in
the command/query/factory layer, not in domain logic.

### Error Handling

- Domain violations → `DomainException` → HTTP 422
- App-level errors → `ApplicationException` with status enum (NOT_FOUND, UNAUTHORIZED, etc.)
- Centralized mapping in `GlobalExceptionHandler`
- Never leak internals in public error responses

### Concurrency

Concurrent use cases on the same match use per-resource locking via `MatchLockManager`. Keep
operations idempotent. Concurrent tests must assert no duplicated side effects.

## Testing

- Unit tests: domain rules (Truco/Envido logic, scoring, edge cases)
- Architecture tests: ArchUnit validates layer dependencies
- Concurrency tests: `StartMatchCommandHandlerConcurrencyTest`
- Security tests: JWT, protected endpoints, CORS preflight
- Persistence is in-memory — no database setup needed

## Key Files

| Purpose            | Path                                                    |
|--------------------|---------------------------------------------------------|
| Match aggregate    | `domain/model/match/Match.java`                         |
| Round entity       | `domain/model/match/Round.java`                         |
| Use case beans     | `infrastructure/config/UseCaseConfiguration.java`       |
| HTTP controller    | `infrastructure/http/MatchController.java`              |
| WebSocket notifier | `infrastructure/websocket/StompMatchEventNotifier.java` |
| Architecture test  | `test/.../architecture/CleanArchitectureTest.java`      |
| API contracts      | `docs/CONTRATOS_API.md`                                 |
| Coding standards   | `README_CODIFICACION.md`                                |
| Agent guide        | `AGENTS.md`                                             |

## Definition of Done

A task is complete only when:

- Correct layer ownership (no business rules in adapters)
- Tests cover changed behavior (regression tests for bugfixes)
- External contracts/docs updated if API or event payloads changed
