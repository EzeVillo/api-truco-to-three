# Plan de Implementación: Espectar partidas de amigos

**Branch**: `011-friend-spectate` | **Date**: 2026-06-06 | **Spec**: [spec.md](spec.md)

**Entrada**: Especificación de feature desde `specs/010-friend-spectate/spec.md`

## Resumen

Permitir que un usuario especte la partida activa de un amigo confirmado, sin agregar una
preferencia
para deshabilitarlo y sin crear un mecanismo paralelo de spectate.

El enfoque técnico reutiliza la arquitectura actual de espectador:

- el alta de espectador sigue siendo WebSocket-first por `/user/queue/match-spectate` con header
  `matchId`;
- `SpectateMatchCommandHandler`, `Spectatorship`, `SpectatorCountChangedPublisher`,
  `GetSpectateMatchStateQueryHandler`, eventos `SPECTATE_STATE` / `SPECTATE_ERROR` y snapshot REST
  se mantienen como flujo único;
- la única regla nueva de acceso es que una amistad aceptada con cualquiera de los jugadores del
  match cuenta como motivo válido para spectate;
- la lista de amigos expone la referencia de partida espectable para que el cliente pueda iniciar
  el flujo ya existente con `matchId`.

## Contexto Técnico

**Language/Version**: Java 21.

**Primary Dependencies**: Spring Boot, Spring Web, Spring Security OAuth2 Resource Server,
Spring WebSocket/STOMP, Spring Data JPA, Springdoc OpenAPI, ArchUnit, JaCoCo.

**Storage**: PostgreSQL en runtime; H2 en modo PostgreSQL para tests. No requiere nuevas tablas:
reutiliza `social_friendships`, matches existentes y `SpectatorshipRepository` en memoria.

**Testing**: JUnit 5 con `.\gradlew.bat test`. Tests de dominio/aplicación sin Spring para política
de elegibilidad y handlers; tests de infraestructura para controller social, listener STOMP y
repositorios si cambia una query. Títulos de tests en español.

**Target Platform**: Servicio backend JVM con REST + WebSocket/STOMP.

**Project Type**: Web service backend monolítico modular con Clean/Hexagonal + DDD.

**Objetivos de performance**: La evaluación de amistad debe resolverse con una consulta puntual por
par de
jugadores. La lista de amigos puede resolver una referencia espectable por amigo con alcance acotado
a la cantidad de amistades aceptadas del usuario autenticado.

**Restricciones**: Seguir la arquitectura actual de spectate. No agregar endpoint REST de alta de
espectador. No revelar cartas no jugadas ni eventos privados por asiento. No introducir opt-out de
spectate entre amigos confirmados.

**Escala/Alcance**: Cambios acotados a elegibilidad de spectate, contrato de amigos y documentación.
Sin nuevos agregados, sin migraciones, sin canales WebSocket nuevos y sin chat de espectadores.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio                 | Estado | Justificación                                                                                                                                                                                                           |
|---------------------------|--------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **I. Hexagonal + DDD**    | PASS   | La regla de spectate queda en dominio mediante un puerto nuevo de elegibilidad social. La implementación que consulta amistades vive fuera del dominio. Controllers/listeners siguen dependiendo de puertos de entrada. |
| **II. Dominio Puro**      | PASS   | `domain` solo recibe interfaces Java puras y lógica de política; no importa Spring, JPA ni clases del bounded context social.                                                                                           |
| **III. Test-First + 70%** | PASS   | Se planifican tests unitarios de política y handler, además de tests de contrato/controller para el payload social y errores de spectate.                                                                               |
| **IV. Español**           | PASS   | Artefactos Spec Kit y descripciones de tests en español. Código en inglés según convención existente.                                                                                                                   |
| **V. YAGNI**              | PASS   | Se reutiliza el flujo existente de spectate y solo se agrega la elegibilidad por amistad y el `matchId` necesario para entrar desde amigos.                                                                             |

**Resultado**: PASS. No hay violaciones que justificar.

## Estructura del Proyecto

### Documentación (esta feature)

```text
specs/010-friend-spectate/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── friend-spectate.md
├── checklists/
│   └── requirements.md
└── tasks.md              # Lo crea /speckit-tasks, no /speckit-plan
```

### Código Fuente (raíz del repositorio)

Estructura Hexagonal existente. Archivos previstos a crear (✚) o modificar (✎):

```text
src/main/java/com/villo/truco/
├── domain/
│   ├── ports/
│   │   └── FriendshipSpectateEligibilityResolver.java      ✚ puerto puro para amistad aceptada
│   └── model/spectator/
│       ├── SpectatingEligibilityPolicy.java                ✎ acepta competencia o amistad
│       └── SpectatorshipStopReason.java                    ✎ agrega FRIENDSHIP_REMOVED si se corta sesión
├── application/usecases/commands/
│   ├── SpectateMatchCommandHandler.java                    ✎ sin cambio funcional esperado
│   └── SpectatorshipLifecycleManager.java                  ✎ reutilizado para corte por amistad removida
├── application/eventhandlers/
│   └── SpectatorCleanupOnFriendshipRemovedEventHandler.java ✚ corta spectate si pierde elegibilidad
├── infrastructure/config/
│   └── SpectatorConfiguration.java                         ✎ inyecta resolver de amistad
└── social/
    ├── application/
    │   ├── dto/
    │   │   ├── FriendSummaryDTO.java                       ✎ agrega spectatableMatch nullable
    │   │   └── SpectatableMatchRefDTO.java                 ✚ id + status
    │   ├── services/
    │   │   ├── SocialViewAssembler.java                    ✎ arma friend summary enriquecido
    │   │   └── SocialFriendshipSpectateEligibilitySupport.java ✚ adapter hacia puerto domain
    │   └── usecases/queries/
    │       └── GetFriendsQueryHandler.java                 ✎ resuelve match espectable por amigo
    └── infrastructure/
        ├── config/
        │   ├── SocialUseCaseConfiguration.java             ✎ bean del adapter social
        │   └── SocialApplicationEventConfiguration.java    ✎ registra cleanup por FRIENDSHIP_REMOVED
        └── http/dto/response/
            └── FriendSummaryResponse.java                  ✎ expone spectatableMatch nullable

src/test/java/com/villo/truco/
├── domain/model/spectator/
│   └── SpectatingEligibilityPolicyTest.java                ✎ cubre amistad aceptada
├── application/eventhandlers/
│   └── SpectatorCleanupOnFriendshipRemovedEventHandlerTest.java ✚ corta solo si ya no es elegible
├── application/usecases/commands/
│   └── SpectateMatchCommandHandlerTest.java                ✎ casos amigo/no amigo
├── infrastructure/websocket/
│   └── SpectateSubscribeEventListenerTest.java             ✎ error si no hay elegibilidad
└── social/
    ├── application/usecases/queries/
    │   └── GetFriendsQueryHandlerTest.java                 ✚ spectatableMatch solo para amigos jugando
    └── infrastructure/http/
        └── FriendshipControllerTest.java                   ✎ payload social enriquecido

docs/
├── CONTRATOS_API.md
└── README.md
```

**Decisión de estructura**: Se mantiene el monolito modular Hexagonal. La lógica de "puede espectar"
sigue centralizada en `SpectatingEligibilityPolicy`; no se crea otro caso de uso para amigos. El
bounded context social aporta una implementación del puerto de amistad aceptada, de forma análoga a
`SocialFriendshipParticipantsSupport` para chat/invitaciones. La superficie social solo publica el
`matchId` necesario para iniciar el flujo existente de spectate.

## Fase 0 - Research

Ver [research.md](research.md). Decisiones clave:

- Amistad aceptada como nuevo motivo de elegibilidad junto a liga/copa.
- Mantener WebSocket-first para el alta de espectador.
- Enriquecer `GET /api/social/friends` con `spectatableMatch` nullable.
- Reaccionar a `FRIENDSHIP_REMOVED` para cortar spectatorship cuando ya no queda elegibilidad.
- No persistir spectatorship ni crear opt-out.

## Fase 1 - Design & Contracts

- [data-model.md](data-model.md): puerto de elegibilidad social, DTO de match espectable, reglas de
  estado y transiciones.
- [contracts/friend-spectate.md](contracts/friend-spectate.md): cambios en `GET /api/social/friends`
  y uso del contrato existente de `/user/queue/match-spectate`.
- [quickstart.md](quickstart.md): validación manual y automatizada del flujo.
- Contexto de agente: `AGENTS.md` apunta a este plan.

## Constitution Check Post-Design

| Principio                 | Estado | Justificación                                                                                                            |
|---------------------------|--------|--------------------------------------------------------------------------------------------------------------------------|
| **I. Hexagonal + DDD**    | PASS   | El diseño usa puertos para cruzar spectate con social; infrastructure configura beans y expone DTOs, sin saltarse capas. |
| **II. Dominio Puro**      | PASS   | El dominio solo conoce `PlayerId`, `MatchId`, `Match` y el nuevo puerto puro; no conoce JPA ni social infrastructure.    |
| **III. Test-First + 70%** | PASS   | Hay pruebas previstas en dominio, aplicación, WebSocket y HTTP social.                                                   |
| **IV. Español**           | PASS   | Artefactos generados en español.                                                                                         |
| **V. YAGNI**              | PASS   | No hay endpoints de alta nuevos, tablas nuevas, preferencia de privacidad ni chat de espectadores.                       |

## Documentación a actualizar

- `docs/CONTRATOS_API.md`: regla de elegibilidad de spectate, `GET /api/social/friends` con
  `spectatableMatch`, errores esperados y nota de que spectate sigue activándose por WebSocket.
- `README.md`: capacidad social de espectar amigos y cola `/user/queue/match-spectate`.

## Complexity Tracking

> No aplica. La Constitution Check pasó sin violaciones.
