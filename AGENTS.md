# AGENTS.md

This file provides guidance to AI coding agents (Codex, Claude Code, etc.) when working with code
in this repository.

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan:
`specs/015-gameplay-recording/plan.md`
<!-- SPECKIT END -->

---

## Reglas del juego (truco-to-three)

Este proyecto es una **variante propia de truco** con una mecánica de puntaje distinta a las
variantes tradicionales (criollo, argentino, uruguayo). Las reglas clave que afectan al producto:

- **Una partida individual se gana llegando a exactamente 3 puntos**. Es lo que da nombre al
  proyecto (`truco-to-three`).
- **Pasarse de 3 puntos hace perder** la partida (regla de "punto exacto"). Esto debe reflejarse en
  la UI del marcador, en validaciones de scoring y en cualquier copy que explique la modalidad.
- Las **series** (cuando se ofrecen) son **mejor de 1, 3 o 5 partidas**. "Mejor de 1" = partida
  única; "mejor de 3" = primero a 2 partidas ganadas; "mejor de 5" = primero a 3 partidas ganadas.
- El formato por defecto de una serie es **"mejor de 3"** cuando hay que elegir un default sensato.

Estas reglas son del dominio del producto, no preferencias visuales: cualquier feature que toque
scoring, formato de partida o selectores de "a cuántas" debe respetarlas.

---

## Idioma de Trabajo (Spec Kit)

Todo artefacto generado por los skills de Spec Kit DEBE estar escrito en **español**: specs,
planes, tareas, checklists, research, data-model, contratos y cualquier otro documento generado.
Los títulos y descripciones de tests propuestos también en español.

El código fuente (clases, métodos, variables) puede permanecer en inglés según la convención
existente del proyecto.

---

## Backend (Java / Spring Boot)

### Comandos esenciales

```bash
# Levantar dependencias locales (PostgreSQL + Adminer)
docker compose up -d

# Ejecutar la aplicación
./gradlew bootRun

# Ejecutar todos los tests
./gradlew test

# Ejecutar una clase de test específica
./gradlew test --tests "com.villo.truco.domain.model.match.MatchTest"

# Build completo (incluye verificación de cobertura ≥ 70%)
./gradlew build

# Ver reporte de cobertura
# open build/reports/jacoco/test/html/index.html
```

API docs disponibles en `http://localhost:8080/swagger-ui/index.html` al correr localmente.

### Arquitectura (Clean / Hexagonal + DDD)

Enforced en build time via **ArchUnit** (`CleanArchitectureTest.java`). La regla de dependencia es
estricta y unidireccional:

```
Domain → Application → Infrastructure
```

- **Domain** (`com.villo.truco.domain`): Java puro, cero Spring. Contiene agregados, value objects,
  domain events e interfaces de puertos de salida.
- **Application** (`com.villo.truco.application`): Orquesta casos de uso. Contiene
  command/query handlers, interfaces de puertos de entrada, DTO assemblers y handlers de eventos de
  aplicación. No se permiten imports de Spring framework.
- **Infrastructure** (`com.villo.truco.infrastructure`): Wiring con Spring Boot, entidades JPA,
  controllers REST, config WebSocket, seguridad, schedulers e implementaciones de repositorios.

#### Reglas arquitectónicas clave (enforced por ArchUnit)

- Los controllers HTTP deben depender de **interfaces de puertos de entrada**, nunca directamente de
  las implementaciones de casos de uso.
- Los agregados de dominio (Match, Bot, etc.) no deben importarse entre sí — la comunicación
  cross-domain va por domain events.

#### Comunicación en tiempo real

WebSocket/STOMP para actualizaciones del juego:

- Conexión: `/ws` (nativo) o `/ws-sockjs` (fallback SockJS)
- Colas de usuario: `/user/queue/match`, `/user/queue/league`, `/user/queue/cup`,
  `/user/queue/chat`, `/user/queue/social`, `/user/queue/profile`, `/user/queue/match-spectate`
- Topics públicos: `/topic/public-match-lobby`, `/topic/public-league-lobby`,
  `/topic/public-cup-lobby`

### Testing

- Tests usan **H2 in-memory (modo PostgreSQL)** — no requieren Docker.
- Flyway está deshabilitado en tests; DDL es `create-drop`.
- JaCoCo enforce **cobertura mínima del 70%** (configurable via propiedad `coverageMinimum` de
  Gradle).
- Los tests de arquitectura en `CleanArchitectureTest` corren como parte del suite y fallan si se
  viola el layering.

### Configuración local

Todos los defaults están en `src/main/resources/application.yaml`. Variables de entorno clave (todas
tienen defaults funcionales para dev local):

| Variable            | Default                               | Propósito       |
|---------------------|---------------------------------------|-----------------|
| `TRUCO_DB_USER`     | `truco`                               | Usuario DB      |
| `TRUCO_DB_PASSWORD` | `truco`                               | Password DB     |
| `TRUCO_JWT_SECRET`  | `truco-local-dev-secret-key-32-bytes` | Clave JWT       |

Docker Compose levanta PostgreSQL en el puerto `5432` y Adminer en `8081`.

### Convenciones de código Java

- **Siempre usar `import` statements** en lugar de nombres completamente calificados (FQCN) inline.
  Nunca escribir `java.util.stream.Stream<com.villo.truco.domain.ports.Foo>` en el cuerpo del
  código; agregar el import arriba y usar el nombre corto.
  Excepción válida: cuando hay colisión de nombres entre dos clases con el mismo simple name.

- **Timing de emisión de `ApplicationEvent`**: todo evento de aplicación nuevo DEBE declarar su
  momento de emisión implementando exactamente uno de dos marcadores (verificado por ArchUnit en
  `CleanArchitectureTest`):
    - `PostCommitApplicationEvent`: notificaciones al usuario (pushes WebSocket). Se difieren a
      `afterCommit` para evitar race conditions (404 por lectura prematura, avisos fantasma o
      duplicados por rollback/retry).
    - `InTransactionApplicationEvent`: eventos de coordinación que disparan escrituras atómicas en
      otro agregado (avance de liga/copa, logros, etc.). Permanecen dentro de la transacción.

---

## Frontend (Angular)

### Comandos esenciales

```bash
# Instalar dependencias
pnpm install

# Servidor de desarrollo (http://localhost:4200)
pnpm start

# Build de producción
pnpm build

# Tests con Vitest
pnpm test

# Lint
pnpm lint
pnpm lint:fix

# Lint de estilos — SCSS bajo features/ y shared/components/ (verifica colores hardcodeados)
pnpm lint:styles

# Lint de templates — detecta mat-flat-button / mat-raised-button / color="primary|accent|warn"
pnpm lint:themes

# Formatear código
pnpm format
```

> El gestor de paquetes es **pnpm** (v11). No usar npm ni yarn.

### Diseño Responsivo

La aplicación soporta dos tamaños de pantalla. El ancho mínimo soportado es **360 px**.

| Nombre  | Resolución de referencia | Rango de ancho   |
|---------|--------------------------|------------------|
| Mobile  | 360 × 780                | 360 px – 1023 px |
| Desktop | 1440 × 900               | 1024 px+         |

> El modo paisaje en mobile (landscape) no es un caso de uso contemplado en este proyecto.
> No agregar media queries de `max-height` ni sub-breakpoints dentro del rango mobile;
> mobile-first con un único `@media (min-width: 1024px)` para escalar a desktop.

### Stack tecnológico

- **Angular 21** con componentes standalone (sin NgModules)
- **NgRx Signals** (`signalStore`) para estado reactivo de auth
- **NgRx Store + Effects** disponibles en `appConfig` para estado global futuro
- **Angular Material** para UI
- **@stomp/stompjs + SockJS** para WebSocket/STOMP en tiempo real
- **Vitest** como test runner (no Karma)
- **ESLint + Prettier** con Husky en pre-commit (via lint-staged)

### Arquitectura frontend

#### Estructura `src/app/`

```
core/
  guards/       # authGuard: redirige a /auth/login si no autenticado
  interceptors/ # jwtInterceptor: añade Bearer token a cada request HTTP
  models/       # Interfaces de dominio (Player, Room, AuthResponse, ApiError)
  services/     # WebSocketService: singleton para STOMP
  stores/       # AuthStore (NgRx Signals): token + username en localStorage
shared/         # Barrel exports para componentes/pipes/directivas reutilizables
environments/   # environment.ts (dev) / environment.prod.ts
```

#### Estado de autenticación

`AuthStore` (`src/app/core/stores/auth.store.ts`) es el único store de auth. Persiste `auth_token` y
`auth_username` en `localStorage`. Los componentes lo inyectan con `inject(AuthStore)` y leen
señales (`store.token()`, `store.isAuthenticated()`).

El `jwtInterceptor` lee `AuthStore.token()` y añade `Authorization: Bearer <jwt>` a todas las
requests HTTP automáticamente.

#### WebSocket / STOMP

`WebSocketService` (`src/app/core/services/websocket.service.ts`) gestiona la conexión STOMP a
`environment.wsUrl` via SockJS. La autenticación se pasa en el frame `CONNECT` como header
`Authorization: Bearer <jwt>`.

Método clave: `subscribe<T>(destination: string): Observable<T>` — funciona tanto si ya está
conectado como si no (espera la conexión internamente).

#### Backend

Base URL: `http://localhost:8080/api`

Todos los endpoints bajo `/api/**` (excepto auth y refresh) requieren `Bearer <jwt>`.

El contrato completo (REST + WebSocket + enums) está documentado en `docs/CONTRATOS_API.md`. Es la
referencia autoritativa para tipos de payload, `eventType`, enums permitidos y flujos de reconexión.

Puntos críticos del contrato:

- Los enums son **case-sensitive** (ej. `ESPADA`, `QUIERO`, `FALTA_ENVIDO`)
- Las acciones de juego responden `204 No Content` (sin body)
- El lobby público se bootstrapea por REST y se reconcilia con deltas WebSocket
- Spectate se activa **solo por WebSocket** (no hay endpoint REST de alta)
- `expiresAt` en WS de revancha llega en `epochMillis`; en REST en ISO-8601

#### Imágenes de cartas

Las cartas españolas están en `public/cards/` con formato `{número}_{palo}.png` (ej. `1_espada.png`,
`7_oro.png`). El dorso es `dorso.png`. Palos válidos: `copa`, `espada`, `basto`, `oro`.

### Guardarraíles — Reglas Obligatorias

#### 1. Design tokens obligatorios en SCSS de features y shared/components

**Todo** color, espaciado, radio de borde y sombra en los archivos SCSS bajo
`src/app/features/**/*.scss` y `src/app/shared/components/**/*.scss` **debe** usar exclusivamente
tokens CSS del proyecto (`var(--t3-…)`). Está prohibido usar:

- Colores hexadecimales (`#fff`, `#1a1a1a`, etc.)
- Funciones de color literales (`rgb(...)`, `rgba(...)`, `hsl(...)`, `hsla(...)`) directamente como
  valor de propiedad
- Colores nombrados (`red`, `white`, `black`, etc.) — bloqueados por la regla `color-named: "never"`
  de stylelint

Los tokens están definidos en `src/styles.scss`. Si se necesita un valor nuevo, primero **agregar el
token** allí y luego consumirlo.

**Verificación**: `pnpm lint:styles` falla si se introducen colores hardcodeados. Corre
automáticamente en el pre-commit via lint-staged.

#### 2. Validación cruzada con `docs/CONTRATOS_API.md` antes de tipar/consumir endpoints

Antes de tipar un DTO o consumir un endpoint del backend, **verificar campo a campo** contra
`docs/CONTRATOS_API.md`. Esta documentación es la fuente autoritativa del contrato REST + WebSocket.

Reglas específicas:

- `gamesToPlay` en `POST /api/matches/bot` acepta **exactamente** `{1, 3, 5}` (partidas totales de
  la serie). Nunca `2`.
- La función `seriesFormatToGamesToPlay()` en `src/app/core/models/match.models.ts` mapea:
  `BEST_OF_1 → 1`, `BEST_OF_3 → 3`, `BEST_OF_5 → 5`.
- Los tests de contrato en `src/tests/contract/` verifican la paridad entre los tipos TypeScript y
  el doc del contrato.

**Verificación**: `pnpm test` incluye los contract tests.

#### 3. CTAs tematizados — prohibición de botones Material crudos

**Nunca** usar `mat-flat-button`, `mat-raised-button`, `mat-fab`, `mat-mini-fab` ni
`color="primary|accent|warn"` en templates bajo `src/app/features/**` ni
`src/app/shared/components/**`. Usar siempre las variantes tematizadas del producto:

| Variante    | Clase CSS                    | Uso                                               |
|-------------|------------------------------|---------------------------------------------------|
| Primaria    | `t3-btn t3-btn--primary`     | CTA principal ("Crear partida")                   |
| Neutral     | `t3-btn t3-btn--neutral`     | Acción secundaria ("Volver", "Cancelar")          |
| Destructiva | `t3-btn t3-btn--destructive` | Acción peligrosa ("Salir", confirmar eliminación) |

**Verificación**: `pnpm lint:themes` falla si se detecta alguno de los patrones prohibidos. Corre
automáticamente en el pre-commit via lint-staged.

Para CTAs con título y descripción apilados verticalmente:

- Usar `display: flex; flex-direction: column` en el elemento CTA.
- El título va en un `<span class="*-title">` y la descripción en un `<span class="*-subtitle">`.
- Altura máxima sugerida en mobile: ≤ 96 px.

---

## Documentación

Verificar si el cambio propuesto impacta la documentación del proyecto:

- **`README.md`**: ¿agrega o modifica un recurso REST, una capacidad del sistema, un bounded
  context, una tabla, un enum, un flujo operativo o una regla de negocio? Si sí, listarlo
  explícitamente en la sección "Documentación a actualizar" del plan.

- **`docs/CONTRATOS_API.md`**: ¿agrega un endpoint REST nuevo, modifica el shape de uno existente,
  agrega/quita un eventType WebSocket, cambia enums, cambia reglas de negocio expuestas al frontend,
  o vuelve falsa alguna afirmación existente? Si sí, indicar qué sección debe actualizarse y el
  cambio concreto.

---

## Decisiones de dominio y patrones clave

Esta sección documenta decisiones no obvias que afectan al código. Verificar contra el estado actual
del código antes de asumir que siguen vigentes.

### Notificaciones WebSocket: siempre post-commit

Toda notificación en tiempo real al usuario (push WebSocket) DEBE emitirse **después** del commit,
nunca dentro de la transacción. Se logra implementando `PostCommitApplicationEvent` en el evento
(el `TransactionalApplicationEventPublisher` lo difiere a `afterCommit`).

**Dos categorías de race condition que esto evita:**
- **Categoría A (404):** el cliente recibe el push, hace GET del recurso y recibe 404 porque la tx
  aún no commiteó. Reproducido en: partida rápida, rematch confirmado, inicio de liga/copa.
- **Categoría B (fantasma/duplicado):** el push sale antes del commit; si hay rollback o reintento
  (`retryTransactionalPipeline`), el cliente ve eventos que nunca se persistieron o duplicados. Caso
  claro: chat.

**Excepción — eventos de coordinación (categoría C):** los eventos que disparan **escrituras**
atómicas en otro agregado (avance de liga/copa, logros, creación de sesión de rematch) NO se mueven
a post-commit — partirían la atomicidad. Solo la notificación derivada va post-commit.

### Fase de timeout: derivar del MatchStatus, no del tipo de evento

La fase de timeout de un match (LOBBY / PLAY / NONE) DEBE derivarse del `MatchStatus` real,
estampado en `Match.getMatchDomainEvents()`, no del tipo de `DomainEvent`.

**Por qué:** un mismo tipo de evento puede emitirse en distintas fases. El mapeo por tipo de evento
y el de reconciliación pueden divergir, dando tiempos de timeout incorrectos (p. ej. un lobby que
debería durar 300 s recibía 30 s).

**Patrón:** los eventos se estampan con `setMatchStatus(this.status)` en el punto único de drenado.
El handler mapea con `phaseOf(event.getMatchStatus())`. No agregar un `phaseOf(event-type)`.

### Detección de "jugador entra a una partida" (eventos de ocupación)

Para detectar que un jugador humano pasa a estar ocupado en una partida, no alcanza con
`PlayerJoinedEvent` solo:

- `PlayerJoinedEvent` solo se emite cuando un **segundo** jugador se une a un match existente. El
  **creador** nunca lo dispara.
- **Bot match** y **quick match** nunca emiten `PlayerJoinedEvent`; usan `GameStartedEvent`.
- `GameStartedEvent` se emite **por cada juego**; `gameNumber == 1` marca el inicio real del match.

**Regla:** "entró a un match" = `PlayerJoinedEvent` **OR** `GameStartedEvent` con `gameNumber == 1`.

### Rematch: solo para victorias normales en matches casuales

Solo `MatchFinishedEvent` dispara creación de sesión de rematch. Causas alternativas de
terminación NO generan sesión:

| Motivo             | Evento                | Rematch |
|--------------------|-----------------------|---------|
| Victoria normal    | `MatchFinishedEvent`  | ✅ SÍ   |
| Abandono           | `MatchAbandonedEvent` | ❌ NO   |
| Timeout (idle)     | `MatchForfeitedEvent` | ❌ NO   |
| Cancelación        | `MatchCancelledEvent` | ❌ NO   |

Los matches de **liga/copa** y **campaña** tampoco generan rematch (vetados por `RematchVeto`
implementations). TTL configurable: `truco.rematch.duration=PT2M`.

### Logros de envido con "WIN_MATCH": van en SCORE_CHANGED, no en ENVIDO_RESOLVED

Los logros que dicen "WIN_MATCH" pero se ganan vía envido deben evaluarse en la rama
`SCORE_CHANGED` de `AchievementPolicy`, **no** en `ENVIDO_RESOLVED`.

**Por qué:** el orden de emisión en Match es `EnvidoResolvedEvent → ScoreChangedEvent →
MatchFinishedEvent`. En `ENVIDO_RESOLVED` el score todavía es el previo al envido.

**Cómo:** usar `previousScore` para validar estado pre-envido y el guard
`winnerSeat == lastEnvidoWinnerSeat` para confirmar que el `SCORE_CHANGED` provino del envido. El
contexto del envido (`lastEnvidoWinnerSeat`, `lastEnvidoPointsMano/Pie`, `envidoCallsInRound`)
sobrevive hasta el siguiente `onRoundStarted`.

### Modo Campaña

Bounded context `com.villo.truco.campaign`. El jugador sube un ranking fijo de 100 bots (curva
cuadrática, cima ≈ 43.200 pts) desafiando siempre al inmediato superior. Todos los cruces son al
mejor de 5.

- Puntos por victoria: `100 × (gamesGanador − gamesPerdedor)`. Derrota = 0 pts.
- Los bots de campaña tienen IDs `c0000000-...-NNN` y están **ocultos** en `GET /api/bots`.
- Un solo desafío activo a la vez por jugador (`activeChallenge` en `CampaignProgress`).
- Matches de campaña **no generan rematch** (vetados por `CampaignRematchVeto`).
- Endpoints: `GET /api/campaign` y `POST /api/campaign/challenges`.
- Tabla Flyway: `V19__create_campaign_tables.sql`.
