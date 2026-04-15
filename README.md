# Truco a 3 puntos - Backend

Backend de Truco Argentino con DDD estricto, Spring Boot y eventos en tiempo real.
El dominio implementa la variante de "truco a 3": cada game se gana llegando exactamente a `3`
puntos; si un jugador se pasa, pierde ese game. Un match se resuelve al mejor de `1`, `3` o `5`
games.

## Estado del proyecto

- matches entre jugadores
- matches contra bots
- ligas round-robin
- copas single elimination
- lobby publico para partidas, ligas y copas
- spectating de matches de ligas y copas
- chat por recurso
- amistades, invitaciones rapidas y DM efimero entre amigos
- autenticacion con access token corto para usuarios y refresh token rotado
- REST + WebSocket/STOMP
- persistencia con PostgreSQL + Flyway
- health checks, metricas y schedulers de timeout

## Reglas de dominio principales

- Un game de truco se juega a `3` puntos exactos.
- Si un jugador supera `3`, ese game lo gana el rival.
- Un match acepta `gamesToPlay = 1 | 3 | 5`.
- No te podes ir al mazo siendo mano si no hubo truco ni envido cantado.
- En el envido no se puede mentir.
- Si te cantan truco, podes decir `quiero y me voy al mazo`: los puntos del truco los gana el rival,
  y si al sumarlos se pasa de `3`, pierde ese game.
- Si no tenes cartas en la mano, no podes decir `quiero y me voy al mazo`.
- Si la primera mano termina parda, o si se esta jugando la tercera mano y el jugador mano tira el
  ancho de espada, la ronda termina automaticamente y el rival ya no puede cantar truco.
- Ligas:
  `3` a `8` jugadores, todos contra todos.
- Copas:
  `4` a `8` jugadores, eliminacion directa con soporte de byes.
- El chat se crea automaticamente al iniciar un match, liga o copa.

## Capacidades

- Auth:
  register/login con access token corto + refresh token rotado y acceso guest con access token
  largo.
- Match:
  crear salas `PUBLIC` o `PRIVATE`, join compartido por `joinCode`, lobby publico por coleccion,
  autostart al llenarse, iniciar privado manual, jugar carta, cantar/responder truco,
  cantar/responder envido, fold, abandono, consulta de estado del jugador y consulta de estado
  para espectador registrado.
- Bots:
  catalogo de personalidades y creacion de match listo para jugar.
- League:
  creacion `PUBLIC` o `PRIVATE`, join compartido por `joinCode`, lobby publico por coleccion,
  autostart publico al completarse, leave, start privado, tabla, fixture y avance automatico.
- Cup:
  creacion `PUBLIC` o `PRIVATE`, join compartido por `joinCode`, lobby publico por coleccion,
  autostart publico al completarse, leave, start privado, bracket, avance automatico y
  resolucion por forfeit.
- Spectators:
  solo participantes de la misma liga o copa pueden spectear matches en progreso; la vista de
  espectador no expone cartas privadas ni acciones disponibles.
- Chat:
  lectura, envio de mensajes, limite de 50 mensajes y rate limit de 2 segundos por jugador.
- Social:
  solicitudes de amistad por username exacto, invitaciones rapidas a `MATCH`/`LEAGUE`/`CUP` entre
  amigos aceptados y DM efimero por `FRIENDSHIP`. El envio de invitaciones valida que el
  destinatario no este ocupado en otra partida o torneo. La API separa `friendship-requests`
  (pendientes) de `friendships` (amistades aceptadas) y expone jugadores por `username`, no por
  `PlayerId`. `friendshipId` queda solo como identidad interna del agregado y no forma parte del
  contrato publico REST/WebSocket.
- Tiempo real:
  eventos privados por jugador para match, league, cup, chat y social, mas canal de spectate para
  snapshots y eventos publicos del match.

## Arquitectura

El proyecto esta estructurado en capas con enfoque de Clean Architecture:

- `domain`
  reglas del negocio, agregados, value objects, domain events y especificaciones.
- `application`
  comandos, queries, DTOs, puertos y casos de uso.
- `infrastructure`
  HTTP, seguridad, WebSocket, persistencia, schedulers, configuracion y adaptadores externos.

Agregados principales del dominio:

- `match`
- `league`
- `cup`
- `chat`
- `bot`
- `user`

La restriccion de dependencias se valida con ArchUnit en
`src/test/java/com/villo/truco/architecture/CleanArchitectureTest.java`.

## Stack

- Java 21
- Spring Boot 4
- Spring Web
- Spring Security + OAuth2 Resource Server
- Spring WebSocket / STOMP
- Spring Data JPA
- PostgreSQL
- Flyway
- Springdoc OpenAPI
- JUnit 5
- ArchUnit
- Testcontainers
- JaCoCo

## Estructura del proyecto

```text
src/main/java/com/villo/truco
|- domain
|- application
|- infrastructure

src/main/resources
|- application.yaml
|- db/migration
```

## Como correrlo localmente

### Requisitos

- JDK 21
- Docker Desktop o una instancia local de PostgreSQL

### Base de datos recomendada

Levantar PostgreSQL y Adminer con Docker Compose:

```powershell
docker compose up -d
```

Servicios locales:

- API: `http://localhost:8080`
- PostgreSQL: `localhost:5432`
- Adminer: `http://localhost:8081`

Credenciales por defecto:

- DB name: `truco`
- DB user: `truco`
- DB password: `truco`

### Variables de entorno

Valores por defecto definidos en `application.yaml`:

- `TRUCO_DB_USER=truco`
- `TRUCO_DB_PASSWORD=truco`
- `TRUCO_JWT_SECRET=truco-local-dev-secret-key-32-bytes`
- `TRUCO_JWT_ISSUER=truco-api`
- `TRUCO_JWT_AUDIENCE=truco-clients`
- `TRUCO_USER_ACCESS_TOKEN_EXPIRATION_SECONDS=900`
- `TRUCO_GUEST_ACCESS_TOKEN_EXPIRATION_SECONDS=604800`
- `TRUCO_REFRESH_TOKEN_EXPIRATION_SECONDS=2592000`

Properties sociales por defecto:

- `truco.social.invitation-expiration.match=PT10M`
- `truco.social.invitation-expiration.league=PT30M`
- `truco.social.invitation-expiration.cup=PT30M`

### Autenticacion

- `POST /api/auth/register` y `POST /api/auth/login` devuelven:
  `playerId`, `accessToken`, `refreshToken`, `accessTokenExpiresIn`, `refreshTokenExpiresIn`
- `POST /api/auth/refresh` rota siempre el refresh token y devuelve un nuevo par de tokens
- `DELETE /api/auth/logout` revoca solo la sesion asociada al refresh token enviado
- `POST /api/auth/guest` devuelve solo `playerId`, `accessToken` y `accessTokenExpiresIn`
- WebSocket sigue autenticando solo con `accessToken`; si el FE refresca, reconecta con el token
  nuevo

### Ejecutar la aplicacion

En Windows:

```powershell
.\gradlew.bat bootRun
```

En Linux/macOS:

```bash
./gradlew bootRun
```

Flyway corre al iniciar y valida el esquema sobre PostgreSQL.

## Testing y calidad

Ejecutar tests:

```powershell
.\gradlew.bat test
```

Generar build completo:

```powershell
.\gradlew.bat build
```

La suite cubre:

- reglas de dominio de truco, envido, turnos, folds y scoring
- bots y toma de decisiones
- casos de uso de match, chat, cup y league
- persistencia JPA e in-memory
- seguridad HTTP
- WebSocket/STOMP
- configuracion Spring
- arquitectura por capas con ArchUnit

JaCoCo corre sobre `test` y `check` exige cobertura minima configurable.
Si no se define nada, el minimo actual es `0.70`.

Para tests se usa H2 en memoria (`src/test/resources/application-test.yaml`).

## API y tiempo real

Documentacion disponible:

- contratos funcionales para frontend:
  `docs/CONTRATOS_API.md`
- OpenAPI JSON:
  `http://localhost:8080/v3/api-docs`
- Swagger UI:
  `http://localhost:8080/swagger-ui/index.html`

Contrato de errores relevante:

- los enums recibidos como `String` son case-sensitive; si se envia un valor fuera del contrato, la
  API responde `400` con `InvalidEnumValueException` y lista de valores permitidos
- `POST /api/join/{joinCode}` devuelve `409` si otro request ocupó el ultimo lugar antes del retry
  final sobre un recurso publico
- `POST /api/matches`, `POST /api/leagues` y `POST /api/cups` pueden devolver `409` con
  `JoinCodeRegistryConflictException` si se agotan los reintentos internos por colision del
  `joinCode` generado con otro recurso

Recursos REST principales:

- `/api/auth`
- `/api/matches`
- `/api/leagues`
- `/api/cups`
- `/api/chats`
- `/api/social`
- `/api/bots`

## Salas Publicas y Privadas

- `PRIVATE`:
  persiste `joinCode`, no aparece en lobby y al completar cupo conserva el inicio manual.
- `PUBLIC`:
  tambien devuelve `joinCode`, aparece en lobby y al completar cupo autoinicia.
- Join unificado:
  tanto `PUBLIC` como `PRIVATE` se comparten y se resuelven por `POST /api/join/{joinCode}`.
  La resolucion del join se realiza contra un registro global (`join_code_registry`) y garantiza
  que un `joinCode` apunte a un unico recurso entre `MATCH`/`LEAGUE`/`CUP`.
- Lobby publico:
  `GET /api/matches/public`, `GET /api/leagues/public`, `GET /api/cups/public`.
  Usa `limit` y `after` para paginacion cursor-based; `limit` default `20`, maximo `100`.
  Responde `items` + `_links.self`/`_links.next`; cada item expone `_links.join` con el endpoint
  global; no hay `last`, `prev` ni totales.
- Auto-start publico:
  una partida publica pasa a `IN_PROGRESS` al entrar el segundo jugador.
  una liga/copa publica inicia el torneo y crea/linkea los matches hijos al completarse el cupo.
- Usuarios ocupados:
  no pueden listar ni usar el lobby publico mientras tengan match activo o torneo pendiente.
  si quedaron eliminados de una liga/copa en progreso, recuperan elegibilidad para el lobby.

WebSocket/STOMP:

- endpoint nativo: `/ws`
- endpoint SockJS: `/ws-sockjs`
- colas por usuario:
  `/user/queue/match`, `/user/queue/match-spectate`, `/user/queue/league`, `/user/queue/cup`,
  `/user/queue/chat`, `/user/queue/social`
- topics publicos de lobby:
  `/topic/public-match-lobby`, `/topic/public-league-lobby`, `/topic/public-cup-lobby`
  solo emiten deltas `UPSERT`/`REMOVED`; el snapshot inicial del lobby se obtiene via REST.
- spectate:
  el alta de espectador se registra al suscribirse por STOMP a `/user/queue/match-spectate`
  enviando header nativo `matchId`; la API expone `GET /api/matches/{matchId}/spectate` para leer
  el snapshot del espectador ya registrado.

## Observabilidad y operacion

- Actuator expone `health` y `metrics`
- health groups:
  `liveness` y `readiness`
- schedulers de timeout para matches, leagues y cups
- heartbeat de scheduler incluido en readiness
- logging con `requestId` en MDC

Health endpoint:

- `http://localhost:8080/actuator/health`

## Persistencia

La aplicacion usa PostgreSQL en runtime. Tambien conviven algunos adaptadores in-memory para
componentes puntuales del sistema.

## CI/CD

Workflows actuales en `.github/workflows`:

- `ci.yml`
  ejecuta tests en cada push y build luego del test.
- `release.yml`
  publica GitHub Release al pushear tags `v*` con el JAR generado.

## Notas

- El `README` busca dar contexto rapido del sistema.
- El detalle de contratos REST, WS, enums y errores esta en `docs/CONTRATOS_API.md`.
- El DM de `FRIENDSHIP` es efimero: no persiste historial y se recrea vacio tras reiniciar la app.
