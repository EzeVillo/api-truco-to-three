# Normas de Codificacion - Truco Master

Este documento define las normas de codificacion usadas en este proyecto.
No es una guia generica: esta basada en como esta implementado hoy el codigo.

## 1) Objetivo

- Mantener reglas de juego complejas (Truco/Envido) sin romper consistencia.
- Evitar acoplamiento entre dominio y framework.
- Hacer cambios seguros con tests que detecten regresiones de negocio.
- Mantener contratos HTTP/WS estables para frontend.

## 2) Arquitectura y Dependencias (Clean Architecture)

### 2.1 Capas oficiales

- `domain`: reglas de negocio puras, entidades/agregados, value objects, eventos de dominio.
- `application`: casos de uso, comandos/queries, puertos de entrada/salida, resolvers.
- `infrastructure`: HTTP, WebSocket, seguridad, persistencia in-memory, configuracion Spring.

### 2.2 Reglas estrictas de dependencia

Se validan automaticamente con ArchUnit:

- `domain` no depende de `application`, `infrastructure` ni `org.springframework`.
- `application` no depende de `org.springframework`.
- `infrastructure.http` depende de puertos de entrada (`application.ports.in`), no de
  implementaciones de use cases.

Implicacion practica:

- Si una clase necesita Spring (`@RestController`, `@Bean`, etc.), vive en `infrastructure`.
- Si una clase modela reglas del juego, vive en `domain` y no conoce el framework.
- `application` coordina, no modela infraestructura.

## 3) Modelado de Dominio (DDD tactico)

### 3.1 Agregados y entidades

- `Match` es agregado raiz y concentra invariantes de partida.
- `Round` encapsula reglas de turno, Truco/Envido, fold y scoring de ronda.
- Se usan bases comunes (`AggregateBase`, `EntityBase`) para identidad y eventos.

Regla:

- La logica de negocio no se implementa en controllers ni handlers.
- Los handlers invocan metodos del agregado y persisten/publican eventos.

### 3.2 Value Objects y validacion defensiva

Patron aplicado:

- IDs y conceptos del dominio se modelan con tipos propios (`MatchId`, `PlayerId`, `Card`, etc.).
- La validacion se hace en fabrica o constructor de VO.
- Se evita propagar primitivas sin semantica cuando el dominio exige tipo.

Ejemplo real:

- `Card.of(...)` valida palo no nulo y numeros permitidos (1,2,3,4,5,6,7,10,11,12).

### 3.3 Eventos de dominio

- Las entidades acumulan `domainEvents`.
- El agregado emite eventos de cambios relevantes (turno, carta jugada, score, match finalizado,
  etc.).
- `application` publica eventos via `MatchEventNotifier` y luego limpia eventos (
  `clearDomainEvents`).

Norma:

- Los eventos nacen en el dominio, no en infraestructura.
- Infraestructura solo traduce/publica (ejemplo: STOMP/WS).

## 4) Clean Code aplicado en este repositorio

### 4.1 Responsabilidad unica por clase

- Controllers: parseo request, auth contextual de `matchId`, llamada a use case, respuesta HTTP.
- Use case handlers: orquestacion transaccional de una accion concreta.
- Dominio: reglas e invariantes.
- Configuracion: wiring de beans.

### 4.2 Guard clauses y fail fast

Patrones recurrentes:

- `Objects.requireNonNull(...)` en constructores y factories.
- Validaciones de estado al inicio de metodos (`validateStatus`, `validateTurn`,
  `validateMatchInProgress`).
- Excepciones especificas por regla invalida (`NotYourTurnException`, `EnvidoNotAllowedException`,
  etc.).

Regla:

- Validar temprano y abortar con excepcion de dominio o aplicacion clara.

### 4.3 Nombres expresivos y lenguaje ubicuo

- Comandos/casos de uso usan verbos de negocio: `CallTruco`, `RespondEnvido`, `Fold`, `StartMatch`.
- Estados y enums expresan reglas del juego (`TRUCO_IN_PROGRESS`, `ENVIDO_IN_PROGRESS`,
  `FALTA_ENVIDO`).
- Eventos se nombran por su significado de negocio (`TrucoCancelledByEnvidoEvent`,
  `RoundEndedEvent`).

### 4.4 Inmutabilidad donde aporta seguridad

- DTOs y VOs se implementan con `record` cuando aplica.
- Listas expuestas se devuelven inmutables o copiadas (`Collections.unmodifiableList`,
  `new ArrayList<>(...)`).
- En tests se verifica inmutabilidad de cadenas de envido.

### 4.5 Evitar efectos colaterales inesperados

Reglas observadas:

- Orden estandar en handlers de comando:
    1. Resolver agregado.
    2. Ejecutar accion de dominio.
    3. Persistir.
    4. Publicar eventos.
    5. Limpiar eventos.
- Se agregaron guardas para no emitir eventos de ronda despues de `MATCH_FINISHED`.

### 4.6 Logs con contexto util

- `INFO` para hitos de negocio (inicio de game, score changes, fin de match).
- `DEBUG` para trazas detalladas (acciones repetidas idempotentes, inicio de ronda).
- `WARN` para excepciones controladas y autenticacion invalida.
- `ERROR` para excepciones inesperadas.

Norma:

- Loguear con datos de contexto (`matchId`, `playerId`, puntos, estado) sin filtrar secretos.

### 4.7 Manejo de errores consistente

- `ApplicationException` mapea a status de aplicacion (`NOT_FOUND`, `UNAUTHORIZED`,
  `UNPROCESSABLE`).
- `DomainException` mapea a `422 Unprocessable Content`.
- Fallback de excepcion inesperada a `500` con mensaje generico.
- Contrato uniforme `ErrorResponse`.

### 4.8 Sin logica de negocio en adaptadores

- Controladores no deciden reglas de Truco/Envido.
- `AvailableActionsPolicy` y `Round` centralizan las reglas de acciones permitidas.
- Esto evita que backend y frontend diverjan en reglas (validacion + acciones disponibles).

## 5) Normas por capa

### 5.1 Domain

- Cero dependencias de Spring.
- Regla de negocio encapsulada en metodos del agregado/entidad.
- Excepciones especificas por violacion de invariante.
- Eventos emitidos en el punto exacto donde ocurre el hecho de negocio.
- Usar metodos privados para subreglas (`validateFoldAllowed`, `checkRoundFinished`, etc.).

### 5.2 Application

- Use cases expuestos por interfaces en `application.ports.in`.
- Salida por puertos (`MatchRepository`, `MatchEventNotifier`, etc.).
- Handlers finales, pequenos y orientados a una accion.
- Sin anotaciones Spring dentro de handlers.
- Concurrencia explicita cuando la operacion lo requiere (`MatchLockManager` en start match).

### 5.3 Infrastructure

- Controllers dependen de interfaces de use case.
- Configuracion de beans en clases de `config`.
- Seguridad centralizada en `SecurityConfiguration`.
- Persistencia in-memory aislada por interfaces de repositorio.
- WS/STOMP traduce eventos de dominio a contratos de salida.

## 6) Reglas de negocio criticas que NO se deben romper

Estas reglas tienen tests y/o memoria de repo asociada:

- Envido solo se permite en primera mano y bajo condiciones de flujo compatibles.
- No se puede cantar un tercer `ENVIDO`.
- Si Envido se canta durante `TRUCO_IN_PROGRESS` (en TRUCO inicial), se cancela Truco y se emite
  evento dedicado.
- `mano` no puede `fold` en primera mano sin envido resuelto ni truco aceptado.
- No deben emitirse eventos de nueva ronda despues de que la partida termina.
- Preflight CORS de `join` debe pasar (`OPTIONS`) con seguridad habilitada.

Norma de implementacion:

- Toda regla se implementa en dominio y se refleja en acciones disponibles para cliente.

## 7) Concurrencia e idempotencia

- `StartMatch` usa lock por `matchId` para evitar carreras.
- Se testea en ejecuciones repetidas concurrentes.
- Llamadas repetidas despues de iniciar se tratan como idempotentes (sin eventos extra).

Regla:

- Acciones con riesgo de carrera deben tener mecanismo de exclusion y tests dedicados.

## 8) Convenciones de testing

### 8.1 Tipos de test esperados

- Unit tests de dominio para reglas y edge cases.
- Integration tests para seguridad HTTP/CORS.
- Tests de arquitectura con ArchUnit.
- Tests de concurrencia para escenarios criticos.

### 8.2 Estilo de test

- Nombres de test orientados a comportamiento.
- Uso de `@DisplayName` cuando mejora legibilidad del caso.
- Secciones por contexto con `@Nested` cuando el objeto tiene muchos estados (ej. `EnvidoFlowTest`).
- Assertions expresivas con AssertJ.

### 8.3 Regla de regresion

- Cada bug de negocio detectado debe dejar test de regresion.
- Si una regla afecta UI/action list, testear dominio y acciones disponibles.

## 9) API y contratos (HTTP + WebSocket)

- Respetar `docs/CONTRATOS_API.md` como contrato fuente para FE.
- Enums en request se parsean con `Enum.valueOf(...)`: FE debe enviar valores exactos.
- Endpoints protegidos validan pertenencia de token por `matchId`.
- Contratos de error estables (`errorCode`, `message`, `timestamp`).

Norma:

- Si se cambia contrato, actualizar documento de contratos y tests de integracion.

## 10) Checklist de PR (obligatorio)

Antes de mergear, verificar:

- [ ] Respeta capas (sin dependencias prohibidas por arquitectura).
- [ ] Regla de negocio implementada en dominio, no en controller.
- [ ] Excepciones especificas y mensajes claros.
- [ ] Eventos de dominio emitidos y publicados en el flujo correcto.
- [ ] Sin regresiones en acciones disponibles (`AvailableActionsPolicy`) cuando cambia una regla.
- [ ] Tests nuevos o ajustados para cubrir el cambio.
- [ ] Contratos HTTP/WS actualizados si cambia entrada/salida.
- [ ] Logs con contexto suficiente y sin datos sensibles.

## 11) Comandos de verificacion

En Windows:

```powershell
.\gradlew.bat test
```

En Linux/macOS:

```bash
./gradlew test
```

## 12) Notas finales

- Este proyecto prioriza claridad de dominio y consistencia de reglas por sobre optimizaciones
  prematuras.
- Si una solucion "funciona" pero rompe la separacion de capas o duplica reglas en otro lugar, no
  cumple esta guia.