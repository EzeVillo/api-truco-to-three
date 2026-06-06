# Plan de Implementacion: Disponibilidad de amigos para invitaciones

**Branch**: `013-friend-availability` | **Date**: 2026-06-06 | **Spec**: [spec.md](spec.md)

**Input**: Especificacion de feature desde `specs/013-friend-availability/spec.md`

## Resumen

Extender la actividad social de amigos para que la lista indique si cada amistad aceptada puede ser
invitada a una partida, por que esta ocupada cuando no puede, si esta online y si existe una partida
espectable. La decision tecnica reutiliza la superficie social existente: el bootstrap sigue en
`GET /api/social/friendships`, las novedades siguen por `/user/queue/social`, y la regla de
disponibilidad se calcula desde las mismas fuentes que hoy bloquean crear, unirse o aceptar
invitaciones.

La presencia `online` se modela como dato separado y aproximado, derivado de sesiones WebSocket
activas conocidas. No cambia por si misma la disponibilidad para invitar.

## Contexto Tecnico

**Language/Version**: Java 21.

**Primary Dependencies**: Spring Boot, Spring WebSocket/STOMP, Spring Security OAuth2 Resource
Server, Spring Data JPA, Springdoc OpenAPI, ArchUnit, JaCoCo.

**Storage**: PostgreSQL en runtime; H2 en modo PostgreSQL para tests. No se preve agregar tablas:
se reutilizan amistades, invitaciones, matches, torneos, revanchas, cola quick match en memoria y
sesiones WebSocket activas.

**Testing**: JUnit 5 con `.\gradlew.bat test`. Tests de aplicacion para resolver disponibilidad y
motivos; tests de contrato para payload social; tests de infraestructura para snapshot al
suscribirse y entrega STOMP. Titulos de tests en espanol.

**Target Platform**: Servicio backend JVM con REST + WebSocket/STOMP.

**Project Type**: Web service backend monolitico modular con Clean/Hexagonal + DDD.

**Performance Goals**: Reflejar el 95% de cambios visibles de disponibilidad u online en menos de 2
segundos bajo conexion normal. Resolver el snapshot de hasta 100 amigos en menos de 3 segundos.

**Constraints**: No mezclar `online` con `availability`. No exponer estado privado de partidas. No
crear canales WebSocket nuevos. No duplicar el flujo de spectate. No persistir presencia online si
alcanza con sesiones activas.

**Scale/Scope**: Cambios acotados a disponibilidad social de amigos aceptados, contratos,
documentacion y eventos en vivo. Sin cambios en scoring ni reglas de truco-to-three.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio                 | Estado | Justificacion                                                                                                                              |
|---------------------------|--------|--------------------------------------------------------------------------------------------------------------------------------------------|
| **I. Hexagonal + DDD**    | PASS   | La disponibilidad se resuelve en aplicacion mediante servicios/puertos; REST, STOMP y sesiones quedan como adaptadores de infraestructura. |
| **II. Dominio Puro**      | PASS   | No se agregan dependencias de Spring/JPA al dominio; los puertos siguen definiendo queries y capacidad de disponibilidad.                  |
| **III. Test-First + 70%** | PASS   | El plan incluye tests de resolver, casos de uso, contrato de payload y listeners/notificadores STOMP.                                      |
| **IV. Espanol**           | PASS   | Todos los artefactos Spec Kit y titulos de tests propuestos quedan en espanol.                                                             |
| **V. YAGNI**              | PASS   | Se reutilizan endpoint, cola social y fuentes existentes; no hay canal nuevo, tabla nueva ni presencia persistida innecesaria.             |

**Resultado**: PASS. No hay violaciones que justificar.

## Estructura del Proyecto

### Documentacion (esta feature)

```text
specs/013-friend-availability/
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
|-- contracts/
|   `-- friend-availability.md
|-- checklists/
|   `-- requirements.md
`-- tasks.md              # Lo crea /speckit-tasks, no /speckit-plan
```

### Codigo Fuente (raiz del repositorio)

Cambios previstos, sujetos a ajuste durante `/speckit-tasks` e implementacion:

```text
src/main/java/com/villo/truco/
|-- application/
|   |-- usecases/commands/
|   |   `-- PlayerAvailabilityChecker.java                 mod: exponer/reutilizar motivos bloqueantes
|   `-- eventhandlers/
|       `-- FriendActivityMatchEventTranslator.java        mod: emitir disponibilidad social cuando cambie match/spectate
|-- domain/
|   `-- ports/
|       `-- QuickMatchQueuePort.java                       mod: lectura de presencia en cola si hace falta
|-- infrastructure/
|   |-- websocket/
|   |   `-- WebSocketSessionPresenceTracker.java           nuevo: sesiones activas por jugador para online aproximado
|   `-- config/
|       `-- PlayerAvailabilityConfiguration.java           mod: wiring de resolver de disponibilidad
`-- social/
    |-- application/
    |   |-- dto/
    |   |   |-- FriendAvailabilityDTO.java                  nuevo: estado completo por amigo
    |   |   |-- FriendAvailabilityStateDTO.java             nuevo: snapshot completo
    |   |   |-- FriendSummaryDTO.java                       mod: agregar availability/online/busyReason
    |   |   `-- FriendActivityDTO.java                     mod/compat: migrar o adaptar a availability
    |   |-- events/
    |   |   `-- FriendAvailabilityNotification.java         nuevo: notificacion social a STOMP
    |   |-- ports/in/
    |   |   `-- GetFriendAvailabilityUseCase.java           nuevo: snapshot social completo
    |   |-- queries/
    |   |   `-- GetFriendAvailabilityQuery.java             nuevo: query por jugador autenticado
    |   |-- services/
    |   |   |-- FriendAvailabilityResolver.java             nuevo: regla central de disponibilidad social
    |   |   |-- FriendActivityResolver.java                 mod: integrar spectatableMatch al estado nuevo
    |   |   `-- SocialViewAssembler.java                   mod: helpers de payload social
    |   `-- usecases/queries/
    |       |-- GetFriendsQueryHandler.java                 mod: incluir disponibilidad en REST
    |       `-- GetFriendAvailabilityQueryHandler.java      nuevo: snapshot WS/reconciliacion
    `-- infrastructure/
        |-- websocket/
        |   |-- SocialSubscribeEventListener.java           mod: enviar FRIEND_AVAILABILITY_STATE
        |   `-- StompFriendAvailabilityNotificationHandler.java nuevo: entregar deltas
        `-- http/dto/response/
            `-- FriendSummaryResponse.java                 mod: contrato REST ampliado

src/test/java/com/villo/truco/
|-- social/application/services/
|   `-- FriendAvailabilityResolverTest.java                nuevo
|-- social/application/usecases/queries/
|   `-- GetFriendAvailabilityQueryHandlerTest.java         nuevo
|-- social/application/events/
|   `-- FriendAvailabilityContractTest.java                nuevo
|-- social/infrastructure/websocket/
|   |-- SocialSubscribeEventListenerTest.java              mod
|   `-- StompFriendAvailabilityNotificationHandlerTest.java nuevo
|-- social/infrastructure/http/
|   `-- FriendshipControllerTest.java                      mod
`-- infrastructure/websocket/
    `-- WebSocketSessionPresenceTrackerTest.java           nuevo

docs/
|-- CONTRATOS_API.md                                      mod: REST social y eventos FRIEND_AVAILABILITY_*
`-- README.md                                             mod: disponibilidad social en vivo
```

**Decision de estructura**: Se mantiene el monolito modular Hexagonal. La regla de disponibilidad
vive en aplicacion/social y reutiliza puertos existentes; infraestructura solo adapta HTTP, STOMP y
tracking de sesiones. No se crea bounded context nuevo.

## Fase 0 - Research

Ver [research.md](research.md). Decisiones clave:

- Reutilizar `/user/queue/social` y evolucionar `FRIEND_ACTIVITY_*` hacia `FRIEND_AVAILABILITY_*`.
- Calcular `BUSY` desde bloqueos reales para invitacion, no desde una lista visual fija.
- Tratar `online` como presencia aproximada derivada de sesiones WebSocket activas.
- Mantener `spectatableMatch` como dato opcional dentro del estado social.
- Usar un motivo principal deterministico cuando haya varios bloqueos.

## Fase 1 - Design & Contracts

- [data-model.md](data-model.md): modelos de disponibilidad, presencia online, motivo bloqueante y
  snapshot/delta social.
- [contracts/friend-availability.md](contracts/friend-availability.md): payloads REST y WebSocket,
  enums, reglas de compatibilidad y privacidad.
- [quickstart.md](quickstart.md): validacion manual y automatizada.
- Contexto de agente: `AGENTS.md` apunta a este plan.

## Constitution Check Post-Design

| Principio                 | Estado | Justificacion                                                                                                  |
|---------------------------|--------|----------------------------------------------------------------------------------------------------------------|
| **I. Hexagonal + DDD**    | PASS   | El diseno separa resolucion de negocio, puertos y adaptadores HTTP/STOMP.                                      |
| **II. Dominio Puro**      | PASS   | Los cambios de dominio son inexistentes o limitados a puertos/value objects sin imports de framework.          |
| **III. Test-First + 70%** | PASS   | Hay pruebas previstas para reglas, contratos, privacidad, reconexion y tracking de sesiones.                   |
| **IV. Espanol**           | PASS   | Artefactos generados y titulos de tests planificados estan en espanol.                                         |
| **V. YAGNI**              | PASS   | No se agregan tablas ni canales nuevos; online no se persiste salvo que la implementacion demuestre necesidad. |

## Documentacion a actualizar

- `docs/CONTRATOS_API.md`: actualizar `GET /api/social/friendships`, eventos sociales, payloads,
  enums `availability`/`busyReason`, privacidad y flujo recomendado.
- `README.md`: documentar que la lista de amigos muestra disponibilidad para invitar, online y
  spectate opcional.

## Complexity Tracking

> No aplica. La Constitution Check paso sin violaciones.
