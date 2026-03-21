# AGENTS

Operational guide for AI coding agents in this repository.

## 1) Mission

- Implement changes with minimal risk.
- Preserve architecture boundaries.
- Prefer correctness and test coverage over speed.

## 2) Mandatory Layer Rules

- `domain`:
    - Contains business rules and invariants.
    - Must not depend on framework code.
- `application`:
    - Orchestrates use cases through ports.
    - No transport/framework concerns.
- `infrastructure`:
    - HTTP/WS/security/persistence/configuration only.
    - Must not contain core business rules.

If a requested change affects business behavior, start in `domain`.

## 3) Standard Change Flow

1. Locate impacted rule and owning layer.
2. Apply the smallest safe change in the correct layer.
3. Keep side effects ordered:
    - load state
    - execute domain logic
    - persist
    - publish notifications/events
    - clear local events/state buffers when applicable
4. Add or update tests for changed behavior.
5. Validate architecture boundaries are still respected.

## 4) Input And Type Safety

- Convert external primitives (`String`, `int`, etc.) into semantic domain types as early as
  possible (command/query/factory).
- Use fail-fast validation for null/invalid values.

## 5) Error Handling Contract

- Use specific domain/application exceptions.
- Keep external error mapping consistent and centralized.
- Do not leak internal implementation details in public error responses.

## 6) Concurrency And Idempotency

When a use case can run concurrently on the same resource:

- Use per-resource locking.
- Keep operation idempotent for retries/replays.
- Add concurrent tests and assert no duplicated side effects.

## 7) Definition Of Done

A task is done only if all are true:

- Correct layer ownership.
- No duplicated business rule in adapters.
- Tests cover changed behavior (and regression when bugfix).
- External contract/doc updated if API/event payload changed.

## 8) Extended Guidance

For rationale, examples, and reusable cross-project playbook, see:

- `README_CLEAN_CODE.md`
