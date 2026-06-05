# Quickstart: Estado de presencia / ocupación del usuario

**Feature**: `008-user-presence`

Guía rápida para verificar la feature una vez implementada.

## Prerrequisitos

```bash
# Levantar dependencias locales (PostgreSQL + Adminer)
docker compose up -d

# Correr la app
./gradlew bootRun
```

La API queda en `http://localhost:8080`. Swagger UI: `http://localhost:8080/swagger-ui/index.html`.

## Probar el endpoint

Necesitás un JWT válido de un usuario autenticado (obtenido del flujo de login/registro existente).

```bash
TOKEN="<jwt-del-usuario>"

curl -s http://localhost:8080/api/me/presence \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Escenarios de verificación

1. **Usuario libre** → `busy: false` y `match`, `league`, `cup`, `rematch` en `null`.
2. **Usuario con partida en curso** → `match` con `id` y `status` (`WAITING_FOR_PLAYERS` /
   `READY` / `IN_PROGRESS`).
3. **Usuario en liga/copa en progreso** → `league`/`cup` con `id`, `status` y `currentMatchId`
   apuntando a la misma partida que `match.id`.
4. **Usuario con revancha abierta** → `rematch` con `id` y `originMatchId`.
5. **Sin token o token inválido** → `401 Unauthorized`.

```bash
# Caso 5: sin Authorization → 401
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/me/presence
```

### Verificar que es de solo lectura (SC-005)

Consultá el endpoint varias veces seguidas y comprobá que el estado de los recursos del usuario no
cambia (ni se reinician los temporizadores de inactividad de partida/liga/copa/revancha).

## Correr los tests

```bash
# Test unitario del handler
./gradlew test --tests "com.villo.truco.application.usecases.queries.GetUserPresenceQueryHandlerTest"

# Test de integración del endpoint
./gradlew test --tests "com.villo.truco.infrastructure.http.PresenceControllerIT"

# Suite completa + verificación de arquitectura y coverage
./gradlew build
```
