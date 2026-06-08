# Plan de Implementacion: Estado de envio en chat

**Branch**: `014-chat-cooldown-metadata` | **Date**: 2026-06-08 | **Spec**:
[spec.md](spec.md)

**Input**: Especificacion de feature desde `specs/014-chat-cooldown-metadata/spec.md`

## Resumen

Extender la lectura y las confirmaciones de chat para exponer el estado de envio del jugador
autenticado: si puede enviar ahora y, cuando no puede por cooldown, `nextMessageAllowedAt` en epoch
millis. La decision tecnica reutiliza el estado ya existente en el agregado `Chat`
(`lastMessageTimestamps` + `rateLimitCooldown`) y lo proyecta en DTOs de aplicacion e
infraestructura. No se agregan eventos WebSocket periodicos ni persistencia nueva.

## Contexto Tecnico

**Language/Version**: Java 21.

**Primary Dependencies**: Spring Boot, Spring Web, Spring Security OAuth2 Resource Server,
Springdoc OpenAPI, WebSocket/STOMP existente para eventos de chat, ArchUnit y JaCoCo.

**Storage**: Chat en repositorio existente (`ChatRepository` / `ChatQueryRepository`), actualmente
in-memory para esta superficie. No se agregan tablas ni almacenamiento nuevo.

**Testing**: JUnit 5 con `.\gradlew.bat test`. Tests de dominio/aplicacion para calculo de estado de
envio; tests HTTP/contrato para payloads REST; tests de arquitectura existentes.

**Target Platform**: Servicio backend JVM que expone REST y WebSocket/STOMP.

**Project Type**: Web service backend monolitico modular con Clean/Hexagonal + DDD.

**Performance Goals**: Calcular el estado de envio en lecturas y confirmaciones de chat en tiempo
constante por request, sin consultas adicionales externas. Mantener el contrato apto para que el FE
muestre el cooldown con desviacion maxima de 250 ms bajo red normal.

**Constraints**: No emitir cuentas regresivas ni eventos periodicos de cooldown. No convertir el
error de rate limit en fuente de verdad del cooldown. No cambiar participantes, retencion de 50
mensajes, maximo de 500 caracteres ni regla de 2 segundos.

**Scale/Scope**: Cambios acotados a chat REST, DTOs, documentacion y tests. No cambia el payload de
`MESSAGE_SENT` para otros participantes.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio                 | Estado | Justificacion                                                                                                 |
|---------------------------|--------|---------------------------------------------------------------------------------------------------------------|
| **I. Hexagonal + DDD**    | PASS   | La proyeccion del estado vive en dominio/aplicacion; REST solo adapta DTOs y controllers dependen de puertos. |
| **II. Dominio Puro**      | PASS   | El agregado `Chat` seguira sin imports de Spring/JPA; cualquier calculo nuevo usara Java estandar.            |
| **III. Test-First + 70%** | PASS   | El plan incluye tests de dominio/aplicacion y contrato HTTP antes de cerrar implementacion.                   |
| **IV. Espanol**           | PASS   | Plan, research, data-model, contratos, quickstart y titulos de tests propuestos estan en espanol.             |
| **V. YAGNI**              | PASS   | Se reutiliza estado existente; no hay canal nuevo, scheduler, tabla ni countdown server-side.                 |

**Resultado**: PASS. No hay violaciones que justificar.

## Estructura del Proyecto

### Documentacion (esta feature)

```text
specs/014-chat-cooldown-metadata/
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
|-- contracts/
|   `-- chat-send-state.md
|-- checklists/
|   `-- requirements.md
`-- tasks.md              # Lo crea /speckit-tasks, no /speckit-plan
```

### Codigo Fuente (raiz del repositorio)

Cambios previstos, sujetos a ajuste durante `/speckit-tasks` e implementacion:

```text
src/main/java/com/villo/truco/
|-- domain/model/chat/
|   |-- Chat.java                                      mod: exponer estado de envio calculado
|   `-- ChatReadView.java                              mod: incluir estado de envio por solicitante si aplica
|-- application/
|   |-- dto/
|   |   |-- ChatSendStateDTO.java                       nuevo
|   |   |-- ChatMessagesDTO.java                        mod: agregar sendState
|   |   |-- SendMessageResultDTO.java                   nuevo
|   |   `-- SendMessageToParentResultDTO.java          nuevo o consolidado con SendMessageResultDTO
|   |-- assemblers/
|   |   `-- ChatMessagesDTOAssembler.java              mod: calcular/proyectar sendState
|   |-- ports/in/
|   |   |-- SendMessageUseCase.java                     mod: retornar resultado con sendState
|   |   `-- SendMessageToParentUseCase.java            mod: retornar resultado con chatId + sendState
|   `-- usecases/
|       |-- queries/
|       |   |-- GetChatMessagesQueryHandler.java          mod: pasar jugador solicitante al assembler
|       |   `-- GetChatByParentQueryHandler.java        mod: pasar jugador solicitante al assembler
|       `-- commands/
|           |-- SendMessageCommandHandler.java           mod: devolver estado actualizado
|           `-- SendMessageToParentCommandHandler.java  mod: devolver chatId + estado actualizado
`-- infrastructure/http/
    |-- ChatController.java                            mod: POST /messages deja de ser 204 y devuelve body
    `-- dto/response/
        |-- ChatSendStateResponse.java                 nuevo
        |-- ChatMessagesResponse.java                  mod: agregar sendState
        |-- SendMessageResponse.java                   nuevo
        `-- SendMessageToParentResponse.java           mod: agregar sendState

src/test/java/com/villo/truco/
|-- domain/model/chat/
|   `-- ChatTest.java                                  mod: estado antes/despues del cooldown
|-- application/usecases/
|   |-- queries/
|   |   |-- GetChatMessagesQueryHandlerTest.java        mod: snapshot incluye sendState
|   |   `-- GetChatByParentQueryHandlerTest.java       mod: by-parent incluye sendState
|   `-- commands/
|       |-- SendMessageCommandHandlerTest.java          mod: resultado incluye sendState
|       `-- SendMessageCommandHandlerConcurrencyTest.java mod: mantener proteccion concurrente
`-- infrastructure/http/
    `-- ChatControllerTest.java                        nuevo: contrato REST de chat

docs/
|-- CONTRATOS_API.md                                   mod: secciones 7.1, 7.2, 7.3, 7.4 y resumen
`-- README.md                                          mod: mencionar estado de envio/cooldown de chat
```

**Decision de estructura**: Se mantiene el monolito modular Hexagonal. El dominio conserva la regla
de cooldown y expone una lectura calculada; aplicacion arma DTOs y REST adapta el contrato. No se
agrega canal WebSocket ni componente de infraestructura dedicado.

## Fase 0 - Research

Ver [research.md](research.md). Decisiones clave:

- El estado de envio se expone en snapshots REST de chat.
- Las confirmaciones de envio devuelven el mismo estado actualizado.
- El error por rate limit permanece distinguible, pero no contiene metadata de cooldown.
- `nextMessageAllowedAt` se expresa en epoch millis y se omite/null cuando el jugador puede enviar.
- No se agregan eventos periodicos ni cambios al `MESSAGE_SENT` para otros participantes.

## Fase 1 - Design & Contracts

- [data-model.md](data-model.md): modelos `ChatSendState`, `ChatMessagesState`,
  `SendMessageResult` y transiciones de cooldown.
- [contracts/chat-send-state.md](contracts/chat-send-state.md): payloads REST actualizados,
  respuestas de error y flujo recomendado del cliente.
- [quickstart.md](quickstart.md): validacion automatizada y manual de refresh, envio y rate limit.
- Contexto de agente: `AGENTS.md` apunta a este plan.

## Constitution Check Post-Design

| Principio                 | Estado | Justificacion                                                                                     |
|---------------------------|--------|---------------------------------------------------------------------------------------------------|
| **I. Hexagonal + DDD**    | PASS   | Los contratos se satisfacen con DTOs de aplicacion e infraestructura sin saltar capas.            |
| **II. Dominio Puro**      | PASS   | El calculo de cooldown no necesita framework; usa estado del agregado y tipos Java.               |
| **III. Test-First + 70%** | PASS   | Hay pruebas previstas para reglas, queries, commands, REST y regresion de rate limit.             |
| **IV. Espanol**           | PASS   | Artefactos generados y tests propuestos permanecen en espanol.                                    |
| **V. YAGNI**              | PASS   | No se disenaron headers, campos en error, WebSocket extra ni almacenamiento que la spec descarto. |

## Documentacion a actualizar

- `docs/CONTRATOS_API.md`: actualizar 7.1, 7.2, 7.3, 7.4 para `sendState`,
  `nextMessageAllowedAt`, cambio de `POST /api/chats/{chatId}/messages` de `204` a respuesta con
  body, y aclarar que el error `ChatRateLimitExceededException` no es la fuente de cooldown.
- `README.md`: documentar que el chat expone estado de envio/cooldown en snapshots y confirmaciones
  para soportar refresh del frontend.

## Complexity Tracking

> No aplica. La Constitution Check paso sin violaciones.
