# Plan de Implementacion: Actividad en vivo de amigos

**Branch**: `012-friend-activity-push` | **Date**: 2026-06-06 | **Spec**: [spec.md](spec.md)

**Input**: Especificacion de feature desde `specs/011-friend-activity-push/spec.md`

## Resumen

Agregar una vista social en vivo para que la lista de amigos muestre si cada amigo confirmado tiene
una partida espectable, manteniendo consistente el snapshot inicial con las novedades posteriores.

El enfoque tecnico reutiliza la superficie social existente:

- `GET /api/social/friends` sigue siendo el bootstrap de amigos y ya expone `spectatableMatch`
  nullable;
- `/user/queue/social` sigue siendo la cola personal para novedades sociales;
- se agrega un evento social de actividad de amigo para publicar altas y bajas de
  `spectatableMatch`;
- al suscribirse a la cola social, el backend envia un snapshot de actividad para cerrar la carrera
  entre la carga inicial y los deltas;
- el flujo de alta de espectador sigue separado en `/user/queue/match-spectate` con `matchId`.

## Contexto Tecnico

**Language/Version**: Java 21.

**Primary Dependencies**: Spring Boot, Spring WebSocket/STOMP, Spring Security OAuth2 Resource
Server, Spring Data JPA, Springdoc OpenAPI, ArchUnit, JaCoCo.

**Storage**: PostgreSQL en runtime; H2 en modo PostgreSQL para tests. No requiere tablas nuevas:
usa amistades, usuarios y matches existentes.

**Testing**: JUnit 5 con `.\gradlew.bat test`. Tests de aplicacion para traduccion de eventos y
snapshot social; tests de infraestructura para publicacion STOMP y listener de suscripcion. Titulos
de tests en espanol.

**Target Platform**: Servicio backend JVM con REST + WebSocket/STOMP.

**Project Type**: Web service backend monolitico modular con Clean/Hexagonal + DDD.

**Performance Goals**: Reflejar el 95% de cambios visibles de actividad en menos de 2 segundos bajo
conexion normal. Resolver el snapshot de hasta 100 amigos en menos de 3 segundos.

**Constraints**: No duplicar el flujo de spectate. No exponer cartas, acciones privadas ni estado de
ronda en eventos sociales. Mantener `/user/queue/social` como canal social unico.

**Scale/Scope**: Cambios acotados a actividad social de amigos confirmados, contratos y
documentacion. Sin nuevas tablas, sin un socket por amigo y sin cambios en scoring.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio                 | Estado | Justificacion                                                                                                                       |
|---------------------------|--------|-------------------------------------------------------------------------------------------------------------------------------------|
| **I. Hexagonal + DDD**    | PASS   | La actividad social se modela como casos de aplicacion y eventos; infrastructure solo adapta STOMP y listeners.                     |
| **II. Dominio Puro**      | PASS   | No se requiere agregar dependencias de Spring/JPA al dominio; los eventos de match existentes se consumen desde aplicacion.         |
| **III. Test-First + 70%** | PASS   | El plan incluye pruebas unitarias, de aplicacion e infraestructura para snapshot, deltas y privacidad del payload.                  |
| **IV. Espanol**           | PASS   | Artefactos Spec Kit y titulos de tests propuestos quedan en espanol.                                                                |
| **V. YAGNI**              | PASS   | Se reutiliza `/user/queue/social`; no hay nuevas tablas, canales por amigo ni presencia generica fuera de la necesidad de spectate. |

**Resultado**: PASS. No hay violaciones que justificar.

## Estructura del Proyecto

### Documentacion (esta feature)

```text
specs/011-friend-activity-push/
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
|-- contracts/
|   `-- friend-activity-push.md
|-- checklists/
|   `-- requirements.md
`-- tasks.md              # Lo crea /speckit-tasks, no /speckit-plan
```

### Codigo Fuente (raiz del repositorio)

Estructura Hexagonal existente. Archivos previstos a crear (nuevo) o modificar (mod):

```text
src/main/java/com/villo/truco/
|-- application/
|   `-- eventhandlers/
|       `-- FriendActivityMatchEventTranslator.java        nuevo: publica actividad desde eventos de match
|-- infrastructure/
|   |-- config/
|   |   `-- SocialActivityConfiguration.java               nuevo: registra handlers/listeners de actividad
|   `-- websocket/
|       `-- SocialSubscribeEventListener.java              nuevo: envia snapshot al suscribirse a social
`-- social/
    |-- application/
    |   |-- dto/
    |   |   |-- FriendActivityDTO.java                     nuevo: estado por amigo
    |   |   `-- SpectatableMatchRefDTO.java                mod: reutilizado por actividad
    |   |-- events/
    |   |   `-- FriendActivityNotification.java            nuevo: evento de aplicacion hacia STOMP social
    |   |-- ports/in/
    |   |   `-- GetFriendActivityUseCase.java              nuevo: snapshot de actividad social
    |   |-- queries/
    |   |   `-- GetFriendActivityQuery.java                nuevo: query por jugador autenticado
    |   |-- services/
    |   |   |-- FriendActivityResolver.java                nuevo: arma estado con amistades aceptadas + match
    |   |   `-- SocialViewAssembler.java                   mod: helpers de payload social
    |   `-- usecases/queries/
    |       `-- GetFriendActivityQueryHandler.java         nuevo: caso de uso de snapshot
    |-- domain/ports/
    |   `-- FriendshipQueryRepository.java                 mod: agrega busqueda de amigos por jugador si hace falta
    `-- infrastructure/
        |-- websocket/dto/
        |   `-- SocialWsEvent.java                         mod: sin cambio de forma esperado
        `-- persistence/repositories/
            `-- JpaFriendshipRepositoryAdapter.java        mod: soporte de queries necesarias

src/test/java/com/villo/truco/
|-- application/eventhandlers/
|   `-- FriendActivityMatchEventTranslatorTest.java        nuevo: deltas por inicio/fin de match
|-- infrastructure/websocket/
|   `-- SocialSubscribeEventListenerTest.java              nuevo: snapshot al suscribirse
`-- social/
    |-- application/services/
    |   `-- FriendActivityResolverTest.java                nuevo: privacidad y filtro por amistad aceptada
    `-- application/usecases/queries/
        `-- GetFriendActivityQueryHandlerTest.java         nuevo: snapshot consistente

docs/
|-- CONTRATOS_API.md                                      mod: eventos nuevos de social
`-- README.md                                             mod: capacidad social en vivo
```

**Decision de estructura**: Se mantiene el monolito modular Hexagonal. La lectura de actividad vive
en `social.application`; los eventos de match se traducen en aplicacion hacia notificaciones
sociales;
STOMP queda en infrastructure. No se crea bounded context nuevo.

## Fase 0 - Research

Ver [research.md](research.md). Decisiones clave:

- Reutilizar `/user/queue/social` para actividad de amigos.
- Enviar snapshot de actividad al suscribirse para evitar carreras GET/deltas.
- Publicar deltas solo cuando cambia la disponibilidad de `spectatableMatch`.
- Filtrar destinatarios por amistades aceptadas vigentes.
- Mantener spectate como flujo separado.

## Fase 1 - Design & Contracts

- [data-model.md](data-model.md): entidades de lectura de actividad, snapshot y cambio de actividad.
- [contracts/friend-activity-push.md](contracts/friend-activity-push.md): payloads REST existentes y
  nuevos eventos sociales.
- [quickstart.md](quickstart.md): validacion manual y automatizada.
- Contexto de agente: `AGENTS.md` apunta a este plan.

## Constitution Check Post-Design

| Principio                 | Estado | Justificacion                                                                                                  |
|---------------------------|--------|----------------------------------------------------------------------------------------------------------------|
| **I. Hexagonal + DDD**    | PASS   | El diseno cruza match y social mediante eventos de aplicacion y puertos; infrastructure no contiene reglas.    |
| **II. Dominio Puro**      | PASS   | No se agregan imports de frameworks al dominio; los cambios de dominio son inexistentes o limitados a puertos. |
| **III. Test-First + 70%** | PASS   | Hay pruebas previstas para reglas, contrato de payload, publicacion STOMP y casos de reconexion.               |
| **IV. Espanol**           | PASS   | Todos los artefactos de planificacion estan en espanol.                                                        |
| **V. YAGNI**              | PASS   | Se evita un nuevo canal, persistencia de presencia y eventos de detalle innecesarios.                          |

## Documentacion a actualizar

- `docs/CONTRATOS_API.md`: agregar `FRIEND_ACTIVITY_STATE` y `FRIEND_ACTIVITY_CHANGED` bajo social,
  payloads, reglas de privacidad y flujo recomendado de bootstrap/reconexion.
- `README.md`: documentar que la lista de amigos se actualiza en vivo por la cola social y que
  spectate se inicia por el flujo existente.

## Complexity Tracking

> No aplica. La Constitution Check paso sin violaciones.
