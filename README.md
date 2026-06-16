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
- spectating de matches de ligas, copas y amigos confirmados
- chat por recurso
- amistades, invitaciones rapidas y DM efimero entre amigos
- revancha post-partida en matches casuales
- sistema de logros por jugador registrado
- autenticacion con access token corto para usuarios y refresh token rotado
- REST + WebSocket/STOMP
- persistencia con PostgreSQL + Flyway
- health checks, metricas y timeouts instantaneos por entidad

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
- Revancha:
  al terminar un match casual (no perteneciente a liga ni copa), se abre automaticamente una
  sesion de revancha con TTL configurable. El bot oponente acepta siempre automaticamente y no
  puede abandonar. Mientras la sesion esta `OPEN`, ambos jugadores quedan ocupados y no pueden
  crear ni unirse a otra partida, liga, copa ni aceptar invitaciones sociales.

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
  catalogo de personalidades y creacion de match listo para jugar. Tambien partidas **bot vs bot**
  (`POST /api/matches/bot-vs-bot`): dos bots juegan solos, el creador queda ocupado por autoria
  (busy total hasta que termine) y es el unico que puede espectarlas viendo las cartas de ambos.
- League:
  creacion `PUBLIC` o `PRIVATE`, join compartido por `joinCode`, lobby publico por coleccion,
  autostart publico al completarse, leave, start privado, tabla, fixture y avance automatico.
- Cup:
  creacion `PUBLIC` o `PRIVATE`, join compartido por `joinCode`, lobby publico por coleccion,
  autostart publico al completarse, leave, start privado, bracket, avance automatico y
  resolucion por forfeit.
- Spectators:
  participantes de la misma liga/copa y amigos confirmados de alguno de los jugadores pueden
  spectear matches en progreso; la vista de espectador no expone cartas privadas ni acciones
  disponibles.
- Chat:
  lectura, envio de mensajes, limite de 50 mensajes y rate limit de 2 segundos por jugador. Las
  lecturas y confirmaciones de envio exponen `sendState` con `canSendNow` y
  `nextMessageAllowedAt` en epoch millis para reconstruir cooldown despues de un refresh.
- Social:
  solicitudes de amistad por username exacto, invitaciones rapidas a `MATCH`/`LEAGUE`/`CUP` entre
  amigos aceptados y DM efimero por `FRIENDSHIP`. El envio de invitaciones valida que el
  destinatario no este ocupado en otra partida o torneo. La API separa `friendship-requests`
  (pendientes) de `friendships` (amistades aceptadas) y expone jugadores por `username`, no por
  `PlayerId`. En amistades aceptadas, `GET /api/social/friendships` incluye `online`,
  `availability`, `busyReason` y `spectatableMatch`. `availability` indica si se puede invitar al
  amigo; `online` es una senal separada basada en sesiones WebSocket activas. `spectatableMatch.id`
  se usa para entrar por el flujo de spectate existente. La cola `/user/queue/social` envia
  `FRIEND_AVAILABILITY_STATE` al suscribirse y `FRIEND_AVAILABILITY_CHANGED` cuando cambia
  disponibilidad, online o spectate, sin exponer cartas ni estado privado. `friendshipId` queda solo
  como identidad interna del agregado y no forma parte del contrato publico REST/WebSocket.
- Profile:
  tracking de logros en tiempo real para usuarios registrados. Los logros se evalúan por game
  interno a `3` puntos y no se procesan en partidas contra bots. Al registrarse, se crea
  automaticamente el perfil del jugador (logros vacios, stats en cero). El endpoint publico
  `GET /api/profile/{username}` expone logros desbloqueados y estadisticas agregadas
  de partidas PvP humanas (matchesPlayed, matchesWon, matchesLost, winRate), sin repetir
  `username` ni exponer `playerId` en el body. Las stats se actualizan al recibir los eventos
  `MATCH_FINISHED`, `MATCH_ABANDONED` y `MATCH_FORFEITED`; las partidas contra bots no cuentan.
  El endpoint `GET /api/achievements` expone el catálogo completo de logros existentes (sus
  códigos), idéntico para todos e independiente del progreso, para que el frontend sepa qué logros
  existen sin hardcodear la lista; el título y la descripción de cada logro los resuelve el
  frontend a partir del código.
- Campaign:
  modo single-player de progresion contra un ranking fijo de `100` bots ordenados por puntos, con
  el puesto `#1` como meta. Solo usuarios registrados pueden jugarlo (los invitados reciben `401` al
  iniciar un desafio). El jugador arranca con `0` puntos en el fondo del ranking y solo puede
  desafiar al bot inmediatamente superior; alcanzado el `#1` se desbloquea desafiar a cualquier bot.
  Todos los enfrentamientos son al mejor de `5` games (no ofrecen revancha). La victoria otorga
  `100 x (games_ganador - games_perdedor)` puntos (3-0 = 300, 3-1 = 200, 3-2 = 100); la derrota no
  descuenta y los puntos nunca son negativos. La posicion en el ranking se deriva de los puntos:
  para
  superar a un bot hay que tener estrictamente mas puntos que el. La distribucion de puntos de los
  bots sigue una curva cuadratica calibrada para que llegar al `#1` requiera ~24 h de juego
  efectivo.
  Cada cruce queda registrado en un head-to-head por rival (victorias/derrotas). El endpoint
  `GET /api/campaign` devuelve el ranking completo (`100` bots + el jugador intercalado en su
  posicion real), el progreso y que rival es desafiable; `POST /api/campaign/challenges` crea el
  match de campaña contra el rival correspondiente. Al terminar cada match (victoria o derrota) se
  empuja un evento `CAMPAIGN_MATCH_POINTS` por `/user/queue/campaign` con los puntos conseguidos, el
  total acumulado y el movimiento de posicion. Desbloquea los logros `REACH_CAMPAIGN_TOP_ONE`
  (llegar al `#1`) y `DEFEAT_ALL_CAMPAIGN_RIVALS` (ganarle al menos una vez a cada uno de los `100`
  bots; como subir el ranking puede saltear rivales, exige volver despues del `#1` por los
  pendientes). Ademas, tener historial neto `>= 3` a favor (`wins - losses`) contra un bot lo
  **desbloquea de forma permanente para el modo casual** (aparece en la lista `campaignUnlocked` de
  `GET /api/bots` y emite `CAMPAIGN_BOT_UNLOCKED`); las partidas casuales no afectan el head-to-head
  de campaña. Desbloquear los `100` otorga `UNLOCK_ALL_CAMPAIGN_BOTS_IN_CASUAL`.
- Quick Match:
  emparejamiento automatico por `gamesToPlay`. El jugador entra a una cola efimera en memoria; si
  ya hay un oponente esperando con la misma configuracion, se crea una partida `PRIVATE` que arranca
  directamente `IN_PROGRESS` y ambos reciben el `matchId` por su canal `/user/queue/match`. Si no
  hay oponente, el jugador queda en cola indefinidamente hasta cancelar o desconectarse. La
  desconexion del WebSocket retira al jugador de la cola automaticamente.
- Rematch:
  elegir revancha, abandonar sesion, consultar estado. Al confirmar ambos jugadores, el nuevo match
  arranca automaticamente `IN_PROGRESS` (sin necesidad de llamar a `/start`) con asientos invertidos
  y el mismo `gamesToWin` que la partida original; el `newMatchId` llega en el payload WS
  `REMATCH_CONFIRMED`.
- Tiempo real:
  eventos privados por jugador para match, league, cup, chat y social, mas canal de spectate para
  snapshots y eventos publicos del match.
    - Los eventos transicionales del match (cambios de estado observables por todos) se entregan por
      `/user/queue/match` con un campo `stateVersion` monotónicamente creciente.
    - Las notificaciones derivadas por jugador (mano y acciones disponibles) se entregan por
      `/user/queue/match-derived` **sin** `stateVersion`, para no contaminar la secuencia
      transicional.

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
- `profile`
- `rematch`
- `campaign`

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

Properties de revancha por defecto:

- `truco.rematch.duration=PT2M`
- `truco.rematch.batch-size=50`

### Autenticacion

- `POST /api/auth/register` y `POST /api/auth/login` devuelven:
  `playerId`, `username`, `accessToken`, `refreshToken`, `accessTokenExpiresIn`,
  `refreshTokenExpiresIn`
- `POST /api/auth/refresh` rota siempre el refresh token y devuelve un nuevo par de tokens con
  `username`
- `GET /api/auth/me` requiere Bearer token y devuelve `playerId`, `tokenUse` y `username` para
  usuarios registrados (`username: null` para guest). Sirve para rehidratar la sesion tras recargar.
- `DELETE /api/auth/logout` revoca solo la sesion asociada al refresh token enviado
- `POST /api/auth/guest` devuelve solo `playerId`, `accessToken` y `accessTokenExpiresIn`
- El access token mantiene `sub = playerId`; no contiene `username`
- WebSocket sigue autenticando solo con `accessToken`; si el FE refresca, reconecta con el token
  nuevo
- Si el usuario intenta entrar por `joinCode` sin sesion valida, el FE debe conservar el
  `joinCode`/`returnTo`, enviar a login o register, guardar la sesion devuelta y despues ejecutar
  `POST /api/join/{joinCode}`. El registro ya devuelve tokens, por lo que no requiere login
  posterior.

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
- `/api/matches/bot` — `POST` crea una partida contra un bot; `/api/matches/bot-vs-bot` — `POST`
  crea una partida entre dos bots (espectable solo por el creador, que queda ocupado por autoría)
- `/api/matches/quick` — Quick Match: `POST` para entrar a la cola, `DELETE` para cancelar
- `/api/leagues`
- `/api/cups`
- `/api/chats`
- `/api/social`
- `/api/bots` — `GET` devuelve `{ casual, campaignUnlocked }`: los bots casuales y los bots de
  campaña que el jugador desbloqueó (historial neto `>= 3` a favor)
- `/api/campaign` — modo campaña: `GET` devuelve el ranking de `100` bots + el jugador y su
  progreso; `POST /api/campaign/challenges` crea el match al mejor de `5` contra el rival
  desafiable (el inmediato superior, o cualquiera si ya se alcanzó el `#1`)
- `/api/profile`
- `/api/achievements` — catálogo de logros existentes (solo los códigos); el frontend lo cruza con
  `/api/profile/{username}` para mostrar todos los logros con marca de desbloqueado
- `/api/me/presence` — `GET` de solo lectura que devuelve, para el usuario autenticado, en qué
  partida, liga, copa o revancha está ocupado (con los identificadores necesarios para reconectarse
  tras un refresco). No incluye Quick Match porque no sobrevive a la desconexión. Su contraparte en
  tiempo real es la cola `/user/queue/presence`, que **empuja** el mismo snapshot a todas las
  sesiones del usuario cada vez que su ocupación cambia (para sincronizar pestañas/dispositivos y
  derivar a la sesión ociosa); el push complementa, no reemplaza, a este `GET`

## Salas Publicas y Privadas

- `PRIVATE`:
  persiste `joinCode`, no aparece en lobby y al completar cupo conserva el inicio manual.
- `PUBLIC`:
  tambien devuelve `joinCode`, aparece en lobby y al completar cupo autoinicia.
- Reconexion a la sala:
  el `GET` de estado (`/api/matches/{id}`, `/api/leagues/{id}`, `/api/cups/{id}`) devuelve, en fase
  de espera, los datos para reconstruir el lobby (`visibility`, `joinCode`, deadline de la sala). En
  match es un bloque `lobby` anidado, mutuamente excluyente con `roundGame`. Asi, volver a una sala
  tras una desconexion no requiere haber guardado el `joinCode` de la respuesta de creacion.
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
  `/user/queue/chat`, `/user/queue/social`, `/user/queue/profile`, `/user/queue/campaign`,
  `/user/queue/presence`
- topics publicos de lobby:
  `/topic/public-match-lobby`, `/topic/public-league-lobby`, `/topic/public-cup-lobby`
  solo emiten deltas `UPSERT`/`REMOVED`; el snapshot inicial del lobby se obtiene via REST.
- revancha:
  los cinco eventos de revancha (`REMATCH_AVAILABLE`, `REMATCH_OPPONENT_WANTS`,
  `REMATCH_CONFIRMED`, `REMATCH_CLOSED_BY_LEAVE`, `REMATCH_EXPIRED`) viajan en
  `/user/queue/match` con el `matchId` top-level igual al `originMatchId` (el match que termino).
  No hay canal separado para revancha.
- spectate:
  el alta de espectador se registra al suscribirse por STOMP a `/user/queue/match-spectate`
  enviando header nativo `matchId`; la API expone `GET /api/matches/{matchId}/spectate` para leer
  el snapshot del espectador ya registrado. Para amigos, el `matchId` se descubre en
  `GET /api/social/friendships` dentro de `spectatableMatch`. La disponibilidad de ese campo se
  mantiene en vivo por `/user/queue/social` con eventos de disponibilidad de amigos.

## Observabilidad y operacion

- Actuator expone `health` y `metrics`
- health groups:
  `liveness` y `readiness`
- timeouts instantaneos por entidad: cada timeout se programa en el instante exacto de vencimiento
  via `TimeoutScheduler`; al arrancar, `TimeoutReconciliationRunner` reconcilia el estado contra la
  BD y reprograma o dispara inmediatamente los pendientes
- timeouts por fase: la sala de espera y el juego tienen relojes separados.
  match: `lobby-timeout` (default 5 min, cancela la sala) vs `play-timeout` (default 30 s, forfeit
  de turno). liga/copa: solo `lobby-timeout` (default 10 min) en la sala; el torneo en curso no
  tiene timeout propio, el tiempo lo controlan los matches internos
- red de seguridad cada 5 min (`TimeoutSafetyNetScheduler`) que reconcilia de nuevo ante caidas
  no detectadas; no reemplaza el mecanismo principal sino que actua como fallback
- heartbeat de scheduler incluido en readiness (`/actuator/health/schedulerHeartbeat`)
- logging con `requestId` en MDC

Health endpoint:

- `http://localhost:8080/actuator/health`

## Persistencia

La aplicacion usa PostgreSQL en runtime. Tambien conviven algunos adaptadores in-memory para
componentes puntuales del sistema.

### Registro de partidas para entrenamiento de bots

La tabla append-only `match_action_log` (migracion `V22`) guarda, por cada accion jugable que cambia
el estado de una partida (humano y bot: jugar carta, cantar/responder truco, cantar/responder
envido,
irse al mazo), el `MatchSnapshot` completo y sin redactar resultante (columna `match_state` JSONB)
junto con la accion (`action_type` + `action_detail`), el actor (`actor_type` HUMAN/BOT,
`actor_seat`)
y la `schema_version`. El registro corre **post-commit y en su propia transaccion** via un decorator
transparente sobre los 6 use cases de accion, por lo que un fallo de registro nunca afecta la
jugada.
Es append-only e idempotente por `UNIQUE (match_id, state_version)`. No hay endpoint ni export: el
acceso es por consulta SQL directa (rol administrativo/operador de la base), p. ej.
`SELECT ... FROM match_action_log WHERE match_id = :id ORDER BY state_version`.

## CI/CD

Workflows actuales en `.github/workflows`:

- `ci.yml`
  ejecuta tests en cada push y build luego del test.
- `release.yml`
  al pushear tags `v*` (o manualmente): compila el binario nativo (`nativeCompile`), lo valida
  con un smoke test contra PostgreSQL real (readiness, Flyway, springdoc), publica la imagen
  en GHCR (`Dockerfile`) y dispara el deploy hook de Render (secret `RENDER_DEPLOY_HOOK_URL`).

## Build (GraalVM native)

El proyecto se compila a binario nativo con Spring AOT:

- La compilación corre **solo en CI** (necesita 4+ vCPU y ~8 GB de RAM); local alcanza con
  verificar `.\gradlew.bat processAot`.
- Los hints de reflection/proxies que Spring AOT no cubre se registran automáticamente en
  build-time escaneando el código (`infrastructure/aot/*RuntimeHints`): DTOs en paquetes
  `.dto` (payloads WebSocket), arrays del tipo de id de cada `@Entity`, proxies de parámetros
  `@Lazy` sobre interfaces y executors del JDK. Mientras se respeten esas convenciones, los
  agregados futuros quedan cubiertos sin tocar nada.
- Build local (sin gastar pipeline): `docker build -f Dockerfile.local -t
  truco-local .` y correrlo apuntando al Postgres de `docker compose`.
- `Dockerfile` empaqueta el binario ya compilado por el workflow.

## Notas

- El `README` busca dar contexto rapido del sistema.
- El detalle de contratos REST, WS, enums y errores esta en `docs/CONTRATOS_API.md`.
- El DM de `FRIENDSHIP` es efimero: no persiste historial y se recrea vacio tras reiniciar la app.
