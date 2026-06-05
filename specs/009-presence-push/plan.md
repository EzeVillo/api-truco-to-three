# Implementation Plan: Presencia en tiempo real (push de ocupación a sesiones activas)

**Branch**: `009-presence-push` | **Date**: 2026-06-05 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/009-presence-push/spec.md`

## Summary

Empujar, en tiempo real y a **todas las sesiones activas** de un usuario, un **snapshot de presencia
**
(idéntico en forma al de la [008](../008-user-presence/spec.md)) cada vez que su ocupación cambia en
un dominio reconectable (partida, liga, copa, revancha). Así, una sesión ociosa que no fue la que
entró a la partida se entera y deriva al recurso, ya que el usuario no puede hacer otra cosa.

**Enfoque técnico**: se reutiliza el patrón de notificación en tiempo real ya existente en el
bounded
context `social` (domain event → traductor que publica un `ApplicationEvent` → handler STOMP que
empuja a `/user/queue/...`). Concretamente:

1. Se extrae la lógica de resolución de presencia del `GetUserPresenceQueryHandler` (008) a un
   **`UserPresenceResolver`** reutilizable. El handler de la 008 pasa a delegar en él (sin cambio de
   comportamiento).
2. Se agregan **traductores de presencia por dominio** (`MatchPresenceEventTranslator`,
   `LeaguePresenceEventTranslator`, `CupPresenceEventTranslator`, `RematchPresenceEventTranslator`)
   que implementan los `*DomainEventHandler` existentes, filtran los eventos que representan una
   **transición de ocupación** (entrar / avanzar / liberar), determinan los jugadores **humanos**
   afectados (filtrando bots) y, **por cada jugador**, re-resuelven su presencia con
   `UserPresenceResolver` y publican un `PresenceEventNotification`.
3. Un **`StompPresenceNotificationHandler`** (`ApplicationEventHandler<PresenceEventNotification>`)
   empuja el snapshot a `/user/queue/presence` del usuario destinatario.

El payload es el **snapshot completo** (mismos campos que `UserPresenceResponse` de la 008), no un
delta por dominio: garantiza coherencia entre dominios (FR-013), es idempotente (FR-002) y permite
al
FE reconciliar con la consulta pull de la 008 (FR-011). No se crean tablas, ni migraciones, ni
agregados nuevos, ni se toca el dominio.

## Technical Context

**Language/Version**: Java 21 (Spring Boot 3.x)

**Primary Dependencies**: Spring Boot (Web, Messaging/WebSocket-STOMP, Security/JWT, Data JPA),
springdoc-openapi (Swagger), ArchUnit (tests de arquitectura), JaCoCo (coverage).

**Storage**: PostgreSQL (producción); H2 en modo PostgreSQL para tests. Acceso vía JPA/Spring Data.
**Sin nuevas tablas ni migraciones** (solo lecturas sobre repos de consulta existentes).

**Testing**: JUnit 5 + Spring Boot Test. Tests de aplicación sin Spring (traductores con
repos/resolver
fakes). Test de integración WebSocket/STOMP con cliente STOMP de prueba (patrón análogo a
`StompSocialNotificationHandlerTest`). H2 (`create-drop`, Flyway deshabilitado).

**Target Platform**: Servicio web backend (REST + WebSocket/STOMP), desplegado en Linux server.

**Project Type**: Web service (monolito modular con arquitectura Hexagonal + DDD).

**Performance Goals**: Las notificaciones se disparan solo en **transiciones de ocupación** (entrar
a
una partida, arrancar/avanzar un torneo, abrir/cerrar revancha, finalizar partida) — baja frecuencia
respecto del gameplay. Cada transición re-resuelve la presencia del jugador afectado con consultas
indexadas por `playerId`/`status` (las mismas de la 008). No hay broadcast a terceros (solo al
dueño).

**Constraints**:

- **Solo lectura** respecto del dominio (FR-010): los traductores solo **leen** vía repos de
  consulta;
  no modifican agregados ni reinician temporizadores de inactividad.
- **Aislamiento por usuario** (FR-008): cada `PresenceEventNotification` lleva un único destinatario
  y
  su propio snapshot; se entrega a `/user/queue/presence` de ese usuario.
- **Entrega best-effort** (FR-011): si una sesión pierde el push, la consulta pull de la 008
  reconcilia.
- **Bots excluidos**: los jugadores bot presentes en los eventos se filtran (no son sesiones
  humanas).

**Scale/Scope**: 1 `ApplicationEvent` nuevo, 4 traductores de presencia (uno por dominio), 1 handler
STOMP, 1 DTO de evento WS, 1 resolver extraído (refactor de la 008), 1 configuración de wiring, y el
registro de los 4 traductores en los composites existentes. Sin endpoints REST nuevos. Reutiliza el
canal `/user/queue/*` y la infraestructura STOMP ya configurada.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Arquitectura Hexagonal + DDD (NO NEGOCIABLE)**: ✅ Los traductores viven en `application` e
  implementan los puertos de salida `*DomainEventHandler` ya existentes; consumen eventos de
  `domain`
  (dependencia permitida application→domain) y publican un `ApplicationEvent`. El push STOMP vive en
  `infrastructure`. No hay comunicación cruzada entre agregados: cada traductor reacciona a su
  propio
  dominio y la agregación es **orquestación de lectura** sobre puertos de consulta (igual que la
  008).
  No se introducen controllers nuevos.
- **II. Dominio Puro**: ✅ **Cero cambios en `domain`**. No se agregan eventos de dominio, métodos ni
  dependencias de framework. Se reutilizan los eventos y puertos existentes.
- **III. Test-First con Coverage Mínimo (70%)**: ✅ Cobertura por traductor (unitario, con resolver y
  `BotRegistry` fakes), del handler STOMP (mock `SimpMessagingTemplate`), del `UserPresenceResolver`
  extraído, y un test de integración multi-sesión. Títulos en español.
- **IV. Español como Idioma de Trabajo (NO NEGOCIABLE)**: ✅ Todos los artefactos (plan, research,
  data-model, contratos, quickstart) en español. Código en inglés según convención.
- **V. Simplicidad / YAGNI**: ✅ Se **reutiliza** la resolución de la 008 (no se duplica lógica de
  refs) y el patrón social de notificación. Payload = snapshot (no se inventa un protocolo de
  deltas).
  Se notifica solo en transiciones de ocupación (no en cada cambio de status intra-partida: para eso
  ya existe `/user/queue/match`). Sin caché, sin nuevas tablas, sin store de sesiones.

**Resultado**: PASS. Sin violaciones. No se requiere Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/009-presence-push/
├── plan.md                      # Este archivo (/speckit-plan)
├── research.md                  # Fase 0 (/speckit-plan)
├── data-model.md                # Fase 1 (/speckit-plan)
├── quickstart.md                # Fase 1 (/speckit-plan)
├── contracts/
│   └── presence-ws-stream.md    # Contrato del stream /user/queue/presence
├── checklists/
│   └── requirements.md          # Checklist de calidad de la spec
└── tasks.md                     # Fase 2 (/speckit-tasks - NO creado por /speckit-plan)
```

### Source Code (repository root)

Estructura Hexagonal existente. Archivos a crear (✚) o modificar (✎):

```text
src/main/java/com/villo/truco/
├── application/
│   ├── services/  (o usecases/queries/)
│   │   └── UserPresenceResolver.java                   ✚ resuelve UserPresenceDTO por PlayerId
│   ├── usecases/queries/
│   │   └── GetUserPresenceQueryHandler.java            ✎ delega en UserPresenceResolver (008)
│   ├── events/
│   │   └── PresenceEventNotification.java              ✚ ApplicationEvent(recipient, snapshot, ...)
│   └── eventhandlers/
│       ├── MatchPresenceEventTranslator.java           ✚ MatchDomainEventHandler<MatchDomainEvent>
│       ├── LeaguePresenceEventTranslator.java          ✚ LeagueDomainEventHandler<LeagueDomainEvent>
│       ├── CupPresenceEventTranslator.java             ✚ CupDomainEventHandler<CupDomainEvent>
│       └── RematchPresenceEventTranslator.java         ✚ RematchSessionDomainEventHandler<...>
└── infrastructure/
    ├── websocket/
    │   ├── StompPresenceNotificationHandler.java       ✚ ApplicationEventHandler<PresenceEventNotification>
    │   └── dto/
    │       └── PresenceWsEvent.java                    ✚ (eventType, timestamp, payload snapshot)
    └── config/
        ├── PresenceNotificationConfiguration.java      ✚ @Bean traductores + handler STOMP + resolver
        ├── EventNotifierConfiguration.java             ✎ registra los 3 traductores (match/league/cup)
        ├── RematchConfiguration.java                   ✎ registra RematchPresenceEventTranslator
        └── PresenceUseCaseConfiguration.java           ✎ usa UserPresenceResolver en el handler 008

src/test/java/com/villo/truco/
├── application/eventhandlers/
│   ├── MatchPresenceEventTranslatorTest.java           ✚ unitario
│   ├── LeaguePresenceEventTranslatorTest.java          ✚ unitario
│   ├── CupPresenceEventTranslatorTest.java             ✚ unitario
│   └── RematchPresenceEventTranslatorTest.java         ✚ unitario
├── application/services/
│   └── UserPresenceResolverTest.java                   ✚ unitario (o reusa el de la 008)
└── infrastructure/websocket/
    └── StompPresenceNotificationHandlerTest.java       ✚ integración STOMP (multi-sesión)
```

**Structure Decision**: Se mantiene el monolito modular Hexagonal existente. La feature **no crea un
bounded context nuevo**: la presencia es una **vista de lectura agregada** que se proyecta como
notificación. Se replica exactamente el patrón del módulo `social`
([SocialNotificationEventTranslator](../../src/main/java/com/villo/truco/social/application/eventhandlers/SocialNotificationEventTranslator.java)
→ [StompSocialNotificationHandler](../../src/main/java/com/villo/truco/social/infrastructure/websocket/StompSocialNotificationHandler.java)
→ `/user/queue/social`), aplicado a `/user/queue/presence`. Los traductores se enchufan en los
composites de notificadores ya existentes (`EventNotifierConfiguration`, `RematchConfiguration`),
que es el punto de extensión natural para reaccionar a domain events sin tocar los command handlers.

## Documentación a actualizar

- **`README.md`**: nueva capacidad de tiempo real (push de presencia) y nueva cola de usuario
  `/user/queue/presence`. Listar el eventType y la relación con el endpoint `GET /api/me/presence`.
- **`docs/CONTRATOS_API.md`**: agregar el stream WebSocket `/user/queue/presence` (nuevo eventType
  de
  presencia) y la forma del payload (snapshot, idéntico a `UserPresenceResponse`). Aclarar que
  complementa —no reemplaza— a `GET /api/me/presence`.
- **`CLAUDE.md`**: actualizar el puntero del plan actual (entre marcadores `SPECKIT`) a
  `specs/009-presence-push/plan.md`.

## Complexity Tracking

> No aplica. La Constitution Check pasó sin violaciones.
