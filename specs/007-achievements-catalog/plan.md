# Implementation Plan: Catálogo de Logros

**Branch**: `007-achievements-catalog` | **Date**: 2026-06-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/007-achievements-catalog/spec.md`

## Summary

Exponer un endpoint REST de solo lectura `GET /api/achievements` que devuelva el catálogo completo
de logros existentes (sus códigos estables), para que el frontend conozca qué logros existen sin
hardcodear la lista. El perfil (`GET /api/profile/{username}`) no cambia: sigue devolviendo solo los
logros desbloqueados. El frontend cruza catálogo + perfil por `achievementCode` para armar la grilla
"todos con marca de desbloqueado". Los títulos y descripciones los resuelve el frontend; el catálogo
no los incluye. No hay logros ocultos. Se agrega un test de contrato que verifica que
`AchievementCode` y la sección 8.3 de `docs/CONTRATOS_API.md` no se desincronicen.

## Technical Context

**Language/Version**: Java 21 (Spring Boot)

**Primary Dependencies**: Spring Boot (Web, Security/JWT), springdoc-openapi (Swagger). Sin
dependencias nuevas.

**Storage**: N/A para esta feature — el catálogo se deriva del enum de dominio `AchievementCode`, no
se persiste nada nuevo. Los logros desbloqueados ya viven en las tablas de perfil existentes.

**Testing**: JUnit 5 + H2 (PostgreSQL-mode) para el slice MVC; test de contrato que lee
`docs/CONTRATOS_API.md`. JaCoCo ≥ 70% líneas.

**Target Platform**: Servicio web (API REST), Linux/containers.

**Project Type**: Proyecto único backend (Clean/Hexagonal + DDD).

**Performance Goals**: Respuesta efectivamente instantánea; el catálogo es estático (≤ decenas de
elementos) y no toca base de datos.

**Constraints**: Endpoint accesible para cualquier usuario autenticado (mismo criterio que el perfil
existente). Sin cambios en el almacenamiento de jugadores.

**Scale/Scope**: 10 logros hoy; el diseño escala a decenas sin esfuerzo. Un solo endpoint nuevo.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Arquitectura Hexagonal + DDD**: ✅ El controller dependerá de un puerto de entrada
  (`GetAchievementCatalogUseCase`), nunca de la implementación. El handler vive en `application`, el
  enum de catálogo en `domain`, el controller/DTO en `infrastructure`. No se importan agregados
  entre sí.
- **II. Dominio Puro**: ✅ El catálogo se deriva de `AchievementCode` (enum Java puro, ya existente
  en `domain`). No se introduce Spring en el dominio.
- **III. Test-First con Coverage Mínimo**: ✅ Se agregan tests del handler, del controller (slice
  MVC) y el test de contrato (FR-008). Títulos en español.
- **IV. Español como Idioma de Trabajo**: ✅ Todos los artefactos en español; código en inglés según
  convención.
- **V. Simplicidad / YAGNI**: ✅ No se agrega persistencia, ni metadatos (título/descripción), ni
  logros ocultos. El catálogo es la proyección directa de `AchievementCode.values()`. Se crea un
  puerto de entrada mínimo solo porque la arquitectura (Principio I, reforzado por ArchUnit) exige
  que el controller pase por un input port.

**Resultado**: PASS. Sin violaciones que justificar.

## Project Structure

### Documentation (this feature)

```text
specs/007-achievements-catalog/
├── plan.md              # Este archivo
├── research.md          # Fase 0
├── data-model.md        # Fase 1
├── quickstart.md        # Fase 1
├── contracts/           # Fase 1
│   └── get-achievements.md
├── checklists/
│   └── requirements.md  # Creado por /speckit-specify
└── tasks.md             # Fase 2 (/speckit-tasks — NO creado por /speckit-plan)
```

### Source Code (repository root)

```text
src/main/java/com/villo/truco/profile/
├── domain/model/
│   └── AchievementCode.java                    # YA EXISTE — única fuente de verdad del catálogo
├── application/
│   ├── dto/
│   │   └── AchievementCatalogDTO.java          # NUEVO — lista de códigos de logro
│   └── usecases/queries/
│       ├── GetAchievementCatalogUseCase.java   # NUEVO — puerto de entrada
│       └── GetAchievementCatalogQueryHandler.java  # NUEVO — devuelve AchievementCode.values()
└── infrastructure/
    ├── config/
    │   └── ProfileUseCaseConfiguration.java    # MODIFICAR — wiring del nuevo bean
    └── http/
        ├── AchievementController.java          # NUEVO — GET /api/achievements
        └── dto/response/
            └── AchievementCatalogResponse.java # NUEVO — { achievements: [{ achievementCode }] }

src/test/java/com/villo/truco/profile/
├── application/usecases/queries/
│   └── GetAchievementCatalogQueryHandlerTest.java  # NUEVO
└── infrastructure/http/
    ├── AchievementControllerTest.java              # NUEVO (slice MVC)
    └── AchievementCatalogContractTest.java         # NUEVO (FR-008: enum ↔ CONTRATOS_API.md §8.3)

docs/CONTRATOS_API.md                                # MODIFICAR — documentar el nuevo endpoint
README.md                                            # MODIFICAR — listar el recurso REST nuevo
```

**Structure Decision**: Proyecto único backend ya existente. La feature se aloja en el bounded
context `profile`, donde ya viven los logros. Se agrega un controller dedicado
(`AchievementController` en `/api/achievements`) en lugar de colgar el endpoint de `ProfileController`,
porque el catálogo es global (no depende de `{username}`) y mezclarlo confundiría la semántica del
recurso de perfil.

## Documentación a actualizar

- **`docs/CONTRATOS_API.md`**:
  - Agregar la sección del endpoint nuevo `GET /api/achievements` (request sin body, response
    `AchievementCatalogResponse`, auth `bearerAuth`, códigos 200/401).
  - La sección §8.3 (`AchievementCode`) pasa a ser la fuente que valida el test de contrato; dejarla
    como lista canónica.
  - Corregir el ejemplo obsoleto del perfil (sección 7.5.1, línea ~1646) que usa
    `WIN_RETRUCO_FROM_0_0_TO_3`, un código que no existe en el enum, para que el ejemplo use un
    código real.
- **`README.md`**: agregar `GET /api/achievements` al listado de recursos REST / capacidades del
  sistema (catálogo de logros).

## Complexity Tracking

> Sin violaciones de la constitución. No aplica.
