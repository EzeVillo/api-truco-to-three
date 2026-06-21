# AGENTS.md

This file provides guidance to AI coding agents (Codex, Claude Code, etc.) when working with code
in this repository.

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan:
`specs/016-bot-vs-bot-spectating/plan.md`
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

| Variable            | Default                               | Propósito   |
|---------------------|---------------------------------------|-------------|
| `TRUCO_DB_USER`     | `truco`                               | Usuario DB  |
| `TRUCO_DB_PASSWORD` | `truco`                               | Password DB |
| `TRUCO_JWT_SECRET`  | `truco-local-dev-secret-key-32-bytes` | Clave JWT   |

Docker Compose levanta PostgreSQL en el puerto `5432` y Adminer en `8081`.

### Convenciones de código Java

- **Siempre usar `import` statements** en lugar de nombres completamente calificados (FQCN) inline.
  Nunca escribir `java.util.stream.Stream<com.villo.truco.domain.ports.Foo>` en el cuerpo del
  código; agregar el import arriba y usar el nombre corto.
  Excepción válida: cuando hay colisión de nombres entre dos clases con el mismo simple name.

- **No agregar overloads/constructores de conveniencia con defaults ocultos.** Cuando se agrega una
  dimensión nueva a un value object o constructor (p. ej. un campo nuevo en `MatchRules`), NO crear
  un overload retrocompatible que delegue con un valor por defecto solo para no tocar los call sites
  existentes. Ese default implícito esconde la nueva decisión de negocio y hace que el modelo mienta
  sobre qué se decidió dónde. Actualizá explícitamente todos los call sites (tests incluidos) para
  que pasen el valor nuevo de forma visible; preferí un único constructor canónico / factory
  explícito. Aceptá el churn a cambio de que la decisión quede a la vista en cada lugar.

- **Timing de emisión de `ApplicationEvent`**: todo evento de aplicación nuevo DEBE declarar su
  momento de emisión implementando exactamente uno de dos marcadores (verificado por ArchUnit en
  `CleanArchitectureTest`):
    - `PostCommitApplicationEvent`: notificaciones al usuario (pushes WebSocket). Se difieren a
      `afterCommit` para evitar race conditions (404 por lectura prematura, avisos fantasma o
      duplicados por rollback/retry).
    - `InTransactionApplicationEvent`: eventos de coordinación que disparan escrituras atómicas en
      otro agregado (avance de liga/copa, logros, etc.). Permanecen dentro de la transacción.

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

> **Política de memoria (obligatoria).** Está **prohibido** guardar decisiones, información,
> aprendizajes o cualquier contexto del proyecto en la memoria del agente. Todo lo que haya que
> recordar tiene que vivir **dentro del repo**: en este `AGENTS.md` o en un doc bajo `docs/`. Si la
> info es una decisión no obvia que afecta al código, generá un doc en `docs/decisiones/` y
> agregalo a la tabla de abajo (o actualizá el doc/tabla existente que corresponda); si es una
> regla, comando o convención general, va en la sección correspondiente de este archivo.
>
> **Única excepción:** configuración propia de la máquina que **no** debe subirse al repo (paths
> locales, settings personales del harness, credenciales de dev). Eso puede vivir fuera del repo.
>
> **Mantené la doc viva.** Esta documentación (este `AGENTS.md` y los docs bajo `docs/`) tiene que
> reflejar siempre el estado actual del proyecto. Si tocás algo que un doc describe (una regla, un
> flujo, un contrato, una decisión), es parte del mismo cambio ir y **actualizar el doc**. No dejes
> documentación desactualizada: si una afirmación dejó de ser cierta, corregila o borrala en el
> mismo PR.

Decisiones no obvias que afectan al código, documentadas **on-demand** en `docs/decisiones/`. No
hace falta leerlas todas: abrí solo la que corresponde a lo que estás tocando. Cada doc advierte que
hay que verificar contra el estado actual del código antes de asumir que sigue vigente.

| Si tocás…                                                                  | Leé primero                                 |
|----------------------------------------------------------------------------|---------------------------------------------|
| Notificaciones / pushes WebSocket, timing de eventos, recording de jugadas | `docs/decisiones/notificaciones-eventos.md` |
| Timeouts de match (lobby / play)                                           | `docs/decisiones/fase-timeout.md`           |
| Rematch o detección de "jugador ocupado en partida"                        | `docs/decisiones/rematch-ocupacion.md`      |
| Logros / achievements ligados a envido                                     | `docs/decisiones/logros-envido.md`          |
| Bot decision engine / envido del bot                                       | `docs/decisiones/bot-envido.md`             |
| Hardening del bot / debilidad punto-exacto / meta del humano               | `docs/decisiones/bot-hardening.md`          |
| Bot-vs-bot (avance manual) / matches vs bots sin timeout                   | `docs/decisiones/bot-vs-bot.md`             |
| Modo campaña (incluye desbloqueo de bots para casual)                      | `docs/decisiones/campania.md`               |
