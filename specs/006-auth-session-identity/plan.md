# Implementation Plan: Identidad de sesion auth

**Branch**: `006-auth-session-identity` | **Date**: 2026-05-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/006-auth-session-identity/spec.md`

## Summary

Agregar una identidad de sesion autoritativa para que el FE pueda recuperar el `username` de un
usuario registrado despues de recargar, sin depender de datos persistidos localmente. La solucion
incluye dos superficies publicas:

- Las respuestas exitosas de `register`, `login` y `refresh` para usuarios registrados incorporan
  `username`.
- Nuevo endpoint protegido `GET /api/auth/me` para reconstruir la sesion actual desde un Bearer
  token valido.

El access token mantiene la semantica actual: `sub = playerId` y `token_use = user | guest`. El
username se resuelve desde el backend cuando se arma la respuesta, no desde claims del JWT.

## Technical Context

**Language/Version**: Java 21.

**Primary Dependencies**: Spring Boot 4.0.3, Spring Web, Spring Security OAuth2 Resource Server,
Spring Validation, Spring Data JPA, Springdoc OpenAPI.

**Storage**: PostgreSQL en runtime, H2 en modo PostgreSQL para tests. No requiere migracion: se usa
la tabla de usuarios existente.

**Testing**: JUnit 5 via `.\gradlew.bat test`, Spring Boot test, ArchUnit y JaCoCo. No ejecutar
tests por clase; ejecutar la suite completa.

**Target Platform**: Servicio backend JVM.

**Project Type**: Web service backend con Clean/Hexagonal + DDD.

**Performance Goals**: La identidad de sesion se resuelve con una consulta puntual por `playerId`;
debe ser imperceptible para el usuario en bootstrap de sesion.

**Constraints**: Mantener DDD estricto. Application no importa Spring. Controllers dependen de
puertos de entrada. No agregar interfaces con metodos default. No cambiar el subject del JWT.

**Scale/Scope**: Alcance acotado a autenticacion e identidad de sesion. Profile se corrige solo en
documentacion si el contrato estaba desalineado.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio                 | Estado | Justificacion                                                                                                                                             |
|---------------------------|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| **I. Hexagonal + DDD**    | PASS   | El endpoint entra por infrastructure y delega en un puerto de entrada nuevo. La resolucion de username vive en application usando puertos de domain/auth. |
| **II. Dominio Puro**      | PASS   | No se agregan dependencias de framework al dominio. El cambio usa value objects y puertos existentes del bounded context auth.                            |
| **III. Test-First + 70%** | PASS   | Se planifican tests de caso de uso, controller y seguridad HTTP. Titulos en español.                                                                      |
| **IV. Español**           | PASS   | Artefactos Spec Kit en español. El codigo mantiene nombres en ingles por convencion del proyecto.                                                         |
| **V. YAGNI**              | PASS   | No se agrega cambio de token, migracion ni endpoint de profile duplicado. Solo el campo faltante y el bootstrap de sesion.                                |

**Resultado**: PASS. No hay violaciones que justificar.

## Project Structure

### Documentation (this feature)

```text
specs/006-auth-session-identity/
|- plan.md
|- research.md
|- data-model.md
|- quickstart.md
|- contracts/
|  `- auth-session-identity.md
`- tasks.md              # Lo crea /speckit-tasks, no /speckit-plan
```

### Source Code (repository root)

```text
src/main/java/com/villo/truco/auth/
|- application/
|  |- model/
|  |  |- UserAuthenticatedSession.java       # agregar username
|  |  `- AuthenticatedSessionIdentity.java   # nuevo modelo de lectura
|  |- ports/in/
|  |  `- GetCurrentSessionIdentityUseCase.java
|  |- queries/
|  |  `- GetCurrentSessionIdentityQuery.java
|  |- services/
|  |  `- UserSessionIssuer.java              # emitir sesion registrada con username
|  `- usecases/
|     `- queries/
|        `- GetCurrentSessionIdentityQueryHandler.java
|- infrastructure/
|  |- config/
|  |  `- AuthUseCaseConfiguration.java
|  `- http/
|     |- AuthController.java                 # GET /api/auth/me
|     `- dto/response/
|        |- CurrentSessionResponse.java
|        |- LoginResponse.java
|        |- RegisterUserResponse.java
|        `- RefreshUserSessionResponse.java

src/test/java/com/villo/truco/auth/
|- application/usecases/commands/
|  |- LoginCommandHandlerTest.java
|  |- RegisterUserCommandHandlerTest.java
|  `- RefreshUserSessionCommandHandlerTest.java
|- application/usecases/queries/
|  `- GetCurrentSessionIdentityQueryHandlerTest.java
`- infrastructure/http/
   `- AuthControllerTest.java

docs/
|- CONTRATOS_API.md
`- README.md
```

**Structure Decision**: Single project backend. El cambio cruza application e infrastructure dentro
del bounded context `auth`; no necesita tocar agregados de juego ni persistencia.

## Fase 0 - Research

Ver [research.md](research.md). Las decisiones clave son:

- `username` no entra al JWT.
- `GET /api/auth/me` es el endpoint de bootstrap.
- `refresh` resuelve username en backend antes de responder.
- guests tienen identidad explicita pero sin username.

## Fase 1 - Design & Contracts

- [data-model.md](data-model.md): modelos de sesion registrada, identidad actual y guest.
- [contracts/auth-session-identity.md](contracts/auth-session-identity.md): shape REST de auth y
  `GET /api/auth/me`.
- [quickstart.md](quickstart.md): verificacion manual y automatizada.
- Agent context: actualizar el bloque SPECKIT en `AGENTS.md` para apuntar a este plan.

## Constitution Check Post-Design

| Principio                 | Estado | Justificacion                                                                                                            |
|---------------------------|--------|--------------------------------------------------------------------------------------------------------------------------|
| **I. Hexagonal + DDD**    | PASS   | Los contratos REST quedan en infrastructure; la decision de identidad actual se expresa como caso de uso de application. |
| **II. Dominio Puro**      | PASS   | No hay nuevas dependencias tecnicas en domain.                                                                           |
| **III. Test-First + 70%** | PASS   | Hay cobertura planificada por caso de uso y controller; la suite completa valida ArchUnit y JaCoCo.                      |
| **IV. Español**           | PASS   | Documentos generados en español.                                                                                         |
| **V. YAGNI**              | PASS   | No se agregan claims ni persistencia redundante.                                                                         |

## Documentacion a actualizar

- `docs/CONTRATOS_API.md`: auth responses, nuevo `GET /api/auth/me`, flujo recomendado, nota de
  profile case-insensitive y shape real de respuesta.
- `README.md`: seccion Auth y Profile para reflejar `username` en auth, endpoint de sesion actual
  y ruta real `GET /api/profile/{username}`.
