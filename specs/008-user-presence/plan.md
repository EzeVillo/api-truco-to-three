# Implementation Plan: Estado de presencia / ocupación del usuario

**Branch**: `008-user-presence` | **Date**: 2026-06-04 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/008-user-presence/spec.md`

## Summary

Exponer un endpoint REST **de solo lectura** que agrega, para el **usuario autenticado**, su estado
de ocupación en los dominios que sobreviven a una desconexión (partida, liga, copa y revancha),
devolviendo los identificadores necesarios para que el frontend lo reconecte al recurso correcto
tras un refresco o reconexión.

Enfoque técnico: una nueva query (`GetUserPresenceQuery`) cuyo handler orquesta **lecturas** sobre
los puertos de consulta ya existentes (`MatchQueryRepository`, `LeagueQueryRepository`,
`CupQueryRepository`, `RematchSessionRepository`), sin tocar ningún agregado. Se agrega un único
método de consulta nuevo (`MatchQueryRepository.findUnfinishedByPlayer`) y se reutiliza el resto.
El resultado se ensambla en un DTO de aplicación y se expone vía un controller nuevo bajo `/api/me`.

## Technical Context

**Language/Version**: Java 21 (Spring Boot 3.x)

**Primary Dependencies**: Spring Boot (Web, Security/JWT, Data JPA), springdoc-openapi (Swagger),
ArchUnit (tests de arquitectura), JaCoCo (coverage).

**Storage**: PostgreSQL (producción); H2 en modo PostgreSQL para tests. Acceso vía JPA/Spring Data.

**Testing**: JUnit 5 + Spring Boot Test. Tests de dominio/aplicación sin Spring ni Docker; tests de
integración con H2 (`create-drop`, Flyway deshabilitado).

**Target Platform**: Servicio web backend (REST + WebSocket/STOMP), desplegado en Linux server.

**Project Type**: Web service (monolito modular con arquitectura Hexagonal + DDD).

**Performance Goals**: Endpoint puntual de baja frecuencia (1 llamada por carga/reconexión del FE).
Cada dominio se resuelve con consultas indexadas por `playerId`/`status`; sin paginación.

**Constraints**: Operación **estrictamente de solo lectura** (FR-009): no modifica estado ni
reinicia temporizadores de inactividad. Autenticación JWT obligatoria (FR-002).

**Scale/Scope**: Un (1) endpoint REST nuevo, una query + handler, un puerto de entrada, un DTO de
aplicación con 4 sub-referencias, un DTO de respuesta HTTP, un método de repositorio nuevo, su
implementación JPA, y la configuración de wiring. Sin nuevas tablas ni migraciones.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Arquitectura Hexagonal + DDD (NO NEGOCIABLE)**: ✅ El controller depende del puerto de entrada
  `GetUserPresenceUseCase`, nunca del handler concreto. La query vive en `application`, los puertos
  de salida usados están en `domain.ports`, las implementaciones JPA en `infrastructure`. Sin
  comunicación cruzada entre agregados (solo lecturas independientes por dominio).
- **II. Dominio Puro**: ✅ No se agregan dependencias de framework al dominio. El único cambio en
  `domain` es un método nuevo en la interfaz `MatchQueryRepository` (Java puro). No hay lógica de
  negocio nueva en el dominio: la agregación de presencia es orquestación de lectura → vive en
  `application`.
- **III. Test-First con Coverage Mínimo (70%)**: ✅ Se cubrirá el handler (unitario, con repos
  fakes/mocks) y el endpoint (integración con H2 y JWT). Títulos de tests en español.
- **IV. Español como Idioma de Trabajo (NO NEGOCIABLE)**: ✅ Todos los artefactos (plan, research,
  data-model, contratos, quickstart) en español. Código en inglés según convención.
- **V. Simplicidad / YAGNI**: ✅ Se reutilizan los puertos de consulta existentes; se agrega solo el
  método estrictamente faltante (`findUnfinishedByPlayer`). Sin caché, sin nuevos eventos, sin
  WebSocket. Quick match queda explícitamente fuera de alcance (FR-010).

**Resultado**: PASS. Sin violaciones. No se requiere Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/008-user-presence/
├── plan.md              # Este archivo (/speckit-plan)
├── research.md          # Fase 0 (/speckit-plan)
├── data-model.md        # Fase 1 (/speckit-plan)
├── quickstart.md        # Fase 1 (/speckit-plan)
├── contracts/           # Fase 1 (/speckit-plan)
│   └── presence-api.md  # Contrato del endpoint GET /api/me/presence
└── tasks.md             # Fase 2 (/speckit-tasks - NO creado por /speckit-plan)
```

### Source Code (repository root)

Estructura Hexagonal existente. Archivos a crear (✚) o modificar (✎):

```text
src/main/java/com/villo/truco/
├── application/
│   ├── queries/
│   │   └── GetUserPresenceQuery.java                    ✚ record(PlayerId requester)
│   ├── ports/in/
│   │   └── GetUserPresenceUseCase.java                  ✚ extends UseCase<Query, UserPresenceDTO>
│   ├── usecases/queries/
│   │   └── GetUserPresenceQueryHandler.java             ✚ orquesta las 4 lecturas
│   └── dto/
│       ├── UserPresenceDTO.java                         ✚ busy + 4 refs opcionales
│       ├── ActiveMatchRefDTO.java                       ✚ (id, status)
│       ├── ActiveLeagueRefDTO.java                      ✚ (id, status, currentMatchId)
│       ├── ActiveCupRefDTO.java                         ✚ (id, status, currentMatchId)
│       └── ActiveRematchRefDTO.java                     ✚ (id, originMatchId)
├── domain/ports/
│   └── MatchQueryRepository.java                        ✎ + findUnfinishedByPlayer(PlayerId)
└── infrastructure/
    ├── http/
    │   ├── PresenceController.java                      ✚ GET /api/me/presence
    │   └── dto/response/
    │       └── UserPresenceResponse.java                ✚ from(UserPresenceDTO) (+ sub-records)
    ├── config/
    │   └── PresenceUseCaseConfiguration.java            ✚ @Bean del handler
    └── persistence/repositories/
        ├── JpaMatchRepositoryAdapter.java               ✎ implementa findUnfinishedByPlayer
        └── spring/SpringDataMatchRepository.java        ✎ @Query findUnfinishedByPlayer

src/test/java/com/villo/truco/
├── application/usecases/queries/
│   └── GetUserPresenceQueryHandlerTest.java             ✚ unitario (repos fakes)
└── infrastructure/http/
    └── PresenceControllerIT.java                        ✚ integración (H2 + JWT)
```

**Structure Decision**: Se mantiene el monolito modular Hexagonal existente (`domain` →
`application` → `infrastructure`). La feature se aloja en el módulo principal `com.villo.truco`
junto a los demás dominios de juego, siguiendo el patrón exacto de las queries existentes
(p. ej. `GetRematchSessionQuery` / `GetRematchSessionQueryHandler` / `RematchController`). No se
crea un nuevo bounded context: la presencia es una **vista de lectura agregada**, no un agregado
con estado propio.

## Complexity Tracking

> No aplica. La Constitution Check pasó sin violaciones.
