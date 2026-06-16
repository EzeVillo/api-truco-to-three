# Implementation Plan: Partidas Bot vs Bot Espectables

**Branch**: `claude/bot-matches-spectating-4vr917` | **Date**: 2026-06-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/016-bot-vs-bot-spectating/spec.md`

## Summary

Permitir que un usuario autenticado **cree** una partida entre **dos bots** y la **espectee viendo
las cartas en mano de ambos**. La ocupación del usuario deriva de la **autoría** (ser dueño del
match), no del espectado: al crearla queda **busy total** (no puede crear otra ni iniciar otra
actividad) y se libera cuando la partida termina, la haya mirado o no. Solo el **creador** puede
espectar la partida que creó.

**Enfoque técnico:** reutilizar lo que ya existe (motor de bots autónomo, infraestructura de
espectado, disponibilidad/presencia) y agregar una pieza nueva mínima: un **registro de autoría**
`BotVsBotMatchRegistry` (matchId → ownerId) persistido en una tabla propia, **espejando el patrón de
`CampaignMatchRegistry`**. Ese registro es la fuente de verdad para tres cosas: (a) ocupación por
autoría en `PlayerAvailabilityChecker` y `UserPresenceResolver`, (b) elegibilidad owner-only en
`SpectatingEligibilityPolicy`, y (c) veto de revancha. La creación usa `Match.createReady(botOne,
botTwo, rules)` (el agregado ya soporta dos bots) sin tocar el agregado `Match`.

Las manos de ambos bots se entregan al espectador **por WebSocket** (eventos `HAND_DEALT` y
`PLAYER_HAND_UPDATED` de ambos asientos), como a un jugador normal, más el snapshot inicial. De paso,
se **cierra una fuga preexistente**: hoy `HAND_DEALT` (con ambas manos) se reenvía a los espectadores
de cualquier match; tras este cambio solo se reenvía en bot-vs-bot.

**Fuera de alcance:** frontend (solo backend), espectadores adicionales (owner-only) y cancelación
manual anticipada de la partida.

## Technical Context

**Language/Version**: Java 21 (records, sealed, pattern-matching switch), Spring Boot.

**Primary Dependencies**: Spring (solo en infrastructure), JPA/Hibernate, Flyway (migración nueva),
ArchUnit (gate de arquitectura). Reutiliza `BotRegistry`, motor de decisiones de bots,
`SpectatorshipRepository`, `UserPresenceResolver`, `MatchQueryRepository`.

**Storage**: PostgreSQL (prod) / H2 modo PostgreSQL (tests). **Tabla nueva** `bot_vs_bot_matches`
(`match_id` PK, `owner_id`, índice por `owner_id`). Sin cambios de esquema en `matches`.

**Testing**: JUnit + suite Gradle. Tests de dominio sin Spring/Docker (H2). `CleanArchitectureTest`
(ArchUnit) verde. JaCoCo ≥ 70%. Títulos en español.

**Target Platform**: Servidor Linux (backend Spring Boot).

**Project Type**: Web service (backend) — Clean/Hexagonal + DDD.

**Performance Goals**: Volumen bajo (un jugador humano real). Sin objetivos de throughput; la
restricción es no degradar la latencia de las jugadas. La resolución de "match propio activo" en
presencia/disponibilidad debe ser O(1) por consulta (query con join, no escaneo de historial).

**Constraints**:

- El dominio permanece puro: el nuevo puerto `BotVsBotMatchRegistry` vive en `domain.ports`; la
  implementación JPA en `infrastructure`.
- No se modifica el agregado `Match` (ni su entidad/mapper/migración): la autoría se persiste aparte.
- No se importa otro agregado: el registro es un puerto, y `SpectatingEligibilityPolicy` ya recibe
  `Match`.
- La ocupación por autoría debe sobrevivir a "no estar mirando" (anti-infinito).

**Scale/Scope**: Una fila por partida bot-vs-bot creada; a lo sumo una activa por usuario. Sin
particionado ni retención especial.

## Constitution Check

*GATE: pasa antes de Phase 0; re-evaluado tras Phase 1.*

| Principio                                              | Estado | Justificación                                                                                                                                                              |
|--------------------------------------------------------|--------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| I. Hexagonal + DDD (Domain→Application→Infrastructure) | ✅ PASS | Puerto `BotVsBotMatchRegistry` y excepciones en domain; `CreateBotVsBotMatchCommandHandler`, veto, resolver de presencia y assembler en application; adapter/entidad/migración en infrastructure. |
| Controllers dependen de puertos de entrada             | ✅ PASS | `BotController` depende de `CreateBotVsBotMatchUseCase` (puerto in); no de la implementación.                                                                              |
| Agregados no se importan entre sí                      | ✅ PASS | No se importa otro agregado. La autoría es un puerto; la política de espectado ya opera sobre `Match`.                                                                     |
| II. Dominio puro                                       | ✅ PASS | `BotVsBotMatchRegistry` es interfaz POJO; nuevas excepciones POJO. Sin Spring/JPA en domain.                                                                              |
| III. Test-first ≥70%                                   | ✅ PASS | Tests de handler, eligibility owner-only, veto, availability (`OWNS_BOT_MATCH`), presence resolver, assembler (manos) y adapter (H2).                                      |
| IV. Español en artefactos                              | ✅ PASS | spec/plan/research/data-model/quickstart/contracts en español.                                                                                                            |
| V. Simplicidad / YAGNI                                 | ✅ PASS | Reusa spectate/presencia/disponibilidad; registro espeja a campaña; sin tocar `Match` ni el pipeline de redacción por asiento.                                            |

**Resultado**: PASS. Sin violaciones — *Complexity Tracking* no aplica.

**Desvío respecto del input inicial (intencional):** la ocupación NO se modela vía `Spectatorship`
(como sugería la primera iteración), porque la suscripción de espectado se desactiva al cerrar la
sesión WS y permitiría generar partidas infinitas. Se modela por **autoría** persistida, que es la
intención explícita del usuario ("generar el match te deja ocupado, por más que no lo veas").

## Project Structure

### Documentation (this feature)

```text
specs/016-bot-vs-bot-spectating/
├── plan.md              # Este archivo
├── research.md          # Fase 0: decisiones de diseño
├── data-model.md        # Fase 1: entidades, tabla y deltas de DTO
├── quickstart.md        # Fase 1: cómo verificar de punta a punta
├── contracts/
│   └── api.md           # Fase 1: contrato REST + WebSocket (deltas)
└── checklists/
    └── requirements.md  # Checklist de calidad de la spec (ya creado)
```

### Source Code (repository root)

```text
src/main/java/com/villo/truco/
├── domain/
│   ├── ports/
│   │   └── BotVsBotMatchRegistry.java                 # NUEVO — puerto de autoría (matchId→ownerId)
│   └── model/
│       ├── spectator/
│       │   ├── SpectatingEligibilityPolicy.java       # MOD — rama bot-vs-bot owner-only
│       │   └── exceptions/
│       │       └── SpectateBotMatchNotOwnerException.java  # NUEVO (o reuso SpectateNotAllowed)
│       └── match/exceptions/
│           └── PlayerOwnsActiveBotMatchException.java  # NUEVO — motivo de ocupación
├── application/
│   ├── commands/
│   │   └── CreateBotVsBotMatchCommand.java             # NUEVO
│   ├── ports/in/
│   │   └── CreateBotVsBotMatchUseCase.java             # NUEVO
│   ├── dto/
│   │   ├── CreateBotVsBotMatchDTO.java                 # NUEVO
│   │   ├── ActiveOwnedBotMatchRefDTO.java              # NUEVO — ref de presencia
│   │   ├── UserPresenceDTO.java                        # MOD — + ownedBotMatch
│   │   └── SpectatorRoundStateDTO.java                 # MOD — + handPlayerOne/handPlayerTwo
│   ├── usecases/commands/
│   │   ├── CreateBotVsBotMatchCommandHandler.java      # NUEVO
│   │   └── PlayerAvailabilityChecker.java              # MOD — + OWNS_BOT_MATCH
│   ├── usecases/queries/
│   │   └── UserPresenceResolver.java                   # MOD — + ownedBotMatch
│   ├── assemblers/
│   │   └── SpectatorMatchStateDTOAssembler.java        # MOD — manos para bot-vs-bot (snapshot inicial)
│   ├── eventhandlers/
│   │   └── SpectatorNotificationEventTranslator.java   # MOD — reenvía manos por WS solo en bot-vs-bot
│   │                                                   #        + cierra fuga de HAND_DEALT a humanos
│   └── services/ (o ports)/
│       └── BotVsBotRematchVeto.java                    # NUEVO — RematchVeto
└── infrastructure/
    ├── http/
    │   ├── BotController.java                          # MOD — POST /api/matches/bot-vs-bot
    │   └── dto/{request,response}/
    │       ├── CreateBotVsBotMatchRequest.java         # NUEVO
    │       ├── CreateBotVsBotMatchResponse.java        # NUEVO
    │       └── UserPresenceResponse.java               # MOD — + ownedBotMatch
    ├── persistence/
    │   ├── entities/BotVsBotMatchJpaEntity.java        # NUEVO
    │   └── repositories/
    │       ├── JpaBotVsBotMatchRegistryAdapter.java    # NUEVO (impl del puerto)
    │       └── spring/SpringDataBotVsBotMatchRepository.java  # NUEVO
    └── config/
        ├── BotConfiguration.java                       # MOD — bean CreateBotVsBotMatchUseCase
        ├── SpectatorConfiguration.java                 # MOD — eligibility + assembler reciben registry
        ├── PlayerAvailabilityConfiguration.java        # MOD — checker recibe registry
        ├── PresenceNotificationConfiguration.java(*)   # MOD — resolver recibe registry
        ├── EventNotifierConfiguration.java             # MOD — spectator translator recibe registry
        └── MatchUseCaseConfiguration.java              # MOD — bean BotVsBotRematchVeto (lista de vetos)

src/main/resources/db/migration/
└── V22__create_bot_vs_bot_matches.sql                  # NUEVO
```

(*) El bean exacto donde se construye `UserPresenceResolver` se confirma al implementar (presencia
pull 008 / push 009 comparten el resolver).

**Structure Decision**: Backend único Clean/Hexagonal. La feature vive en el contexto de match
(sub-áreas bot/spectator/presence). Una sola pieza de persistencia nueva (registro de autoría) más
ediciones quirúrgicas en disponibilidad, presencia, elegibilidad de espectado y assembler.

## Complexity Tracking

No aplica — Constitution Check sin violaciones.
