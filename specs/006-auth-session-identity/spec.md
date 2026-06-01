# Feature Specification: Identidad de Sesion Auth

**Feature Branch**: `006-auth-session-identity`

**Created**: 2026-05-31

**Status**: Draft

**Input**: User description: "Agregar username a register/login/refresh y endpoint de identidad de
sesion para que el FE reconstruya el usuario autenticado tras recargar sin depender de datos
persistidos stale."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Rehidratar usuario autenticado tras recargar (Priority: P1)

Como cliente frontend con una sesion valida existente, necesito recuperar la identidad visible del
usuario actual despues de recargar la pagina para que la aplicacion muestre el username correcto sin
depender de estado persistido previamente en el cliente.

**Why this priority**: Las sesiones existentes hoy no tienen una forma contractual de recuperar el
username porque los tokens identifican al jugador solo por ID interno.

**Independent Test**: Se puede probar autenticando un usuario registrado, descartando todo estado
local de username mientras se conservan tokens validos, consultando la identidad de la sesion actual
y verificando que el backend devuelva el username.

**Acceptance Scenarios**:

1. **Given** un usuario registrado tiene un access token valido y no hay username guardado
   localmente, **When** el cliente consulta la identidad de la sesion actual, **Then** la respuesta
   incluye player ID, username y tipo de sesion.
2. **Given** un request no tiene access token valido, **When** el cliente consulta la identidad de
   la sesion actual, **Then** el sistema rechaza el request como no autenticado.
3. **Given** un guest tiene un access token valido, **When** el cliente consulta la identidad de la
   sesion actual, **Then** la respuesta identifica la sesion como guest y no inventa un username
   registrado.

---

### User Story 2 - Recibir username durante el ciclo de auth (Priority: P2)

Como cliente frontend que ejecuta register, login o refresh, necesito que cada respuesta exitosa de
auth para usuarios registrados incluya el username autoritativo para inicializar y actualizar la
cache de sesion con datos del backend.

**Why this priority**: Persistir datos solo al login/register es incompleto, y refresh no debe dejar
al frontend con identidad visible faltante o stale.

**Independent Test**: Se puede probar registrando o logueando un usuario y refrescando la sesion,
verificando que cada respuesta de auth para usuario registrado incluya el username autoritativo.

**Acceptance Scenarios**:

1. **Given** un usuario se registra exitosamente, **When** se devuelve la respuesta de auth, **Then
   ** incluye player ID, username, access token, refresh token y expiraciones.
2. **Given** un usuario inicia sesion exitosamente, **When** se devuelve la respuesta de auth, *
   *Then** incluye player ID, username, access token, refresh token y expiraciones.
3. **Given** un usuario refresca una sesion valida, **When** se devuelve la respuesta de refresh, *
   *Then** incluye player ID, username, nuevo access token, nuevo refresh token y expiraciones.

---

### User Story 3 - Mantener contratos publicos alineados (Priority: P3)

Como implementador frontend, necesito que el contrato de API y el README coincidan con el
comportamiento real para tomar decisiones de integracion basadas en garantias documentadas.

**Why this priority**: La documentacion actual omite la garantia faltante de identidad en auth y
contiene detalles de profile desactualizados que pueden inducir errores en la implementacion
frontend.

**Independent Test**: Se puede probar comparando los contratos documentados de auth y profile contra
las formas de respuesta observables.

**Acceptance Scenarios**:

1. **Given** la funcionalidad de identidad de auth esta disponible, **When** el frontend lee la
   documentacion de contratos, **Then** puede identificar como obtener username desde respuestas de
   auth y desde el endpoint de identidad de sesion actual.
2. **Given** se lee la documentacion de profile, **When** describe parametros de ruta y campos de
   respuesta, **Then** coincide con el comportamiento publico actual de profile.

### Edge Cases

- Los refresh tokens existentes creados antes de este cambio deben seguir refrescando exitosamente y
  devolver username cuando el usuario todavia existe.
- Si no se puede resolver el registro de un usuario registrado durante una consulta de sesion actual
  o refresh, el request debe fallar en vez de devolver identidad parcial.
- Las respuestas de auth guest siguen limitadas a datos de identidad guest y no deben implicar un
  username persistente.
- La busqueda de username y la documentacion deben describir de forma consistente el comportamiento
  real case-insensitive.
- Si el FE intenta entrar por `joinCode` sin una sesion autenticada valida, debe conservar la
  intencion de join y retomarla despues de login o register. Register ya devuelve sesion completa,
  por lo que no requiere login posterior.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST proveer una respuesta protegida de identidad de sesion actual para
  cualquier access token valido.
- **FR-002**: La respuesta de identidad de sesion actual para usuarios registrados MUST incluir
  player ID, username y tipo de sesion.
- **FR-003**: La respuesta de identidad de sesion actual para guests MUST incluir player ID y tipo
  de sesion, y MUST hacer explicita la ausencia de username.
- **FR-004**: Las respuestas exitosas de registro de usuarios registrados MUST incluir username
  ademas de los datos existentes de tokens y expiraciones.
- **FR-005**: Las respuestas exitosas de login de usuarios registrados MUST incluir username ademas
  de los datos existentes de tokens y expiraciones.
- **FR-006**: Las respuestas exitosas de refresh de usuarios registrados MUST incluir username
  ademas de los datos existentes de tokens y expiraciones.
- **FR-007**: El sistema MUST preservar la semantica existente de tokens: el subject del access
  token sigue siendo el player ID y la distincion registered/guest sigue disponible.
- **FR-008**: El sistema MUST rechazar consultas de identidad de sesion actual sin access token
  valido.
- **FR-009**: La documentacion publica para frontend MUST describir los nuevos campos de identidad y
  el endpoint de sesion actual.
- **FR-010**: El README y la documentacion de contrato MUST alinearse con la ruta y forma de
  respuesta actual de profile.
- **FR-011**: La documentacion publica para frontend MUST describir el flujo recomendado para
  retomar un join pendiente despues de login o register.

### Key Entities

- **Identidad de sesion autenticada**: Identidad autoritativa del backend para el caller actual,
  incluyendo player ID, tipo de sesion y username cuando el caller es un usuario registrado.
- **Respuesta de auth registrada**: Respuesta devuelta despues de register, login y refresh para una
  sesion de usuario persistido.
- **Identidad de sesion guest**: Identidad efimera de jugador que no tiene username registrado ni
  refresh token.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% de las respuestas exitosas de register, login y refresh para usuarios registrados
  incluyen un username autoritativo.
- **SC-002**: 100% de las sesiones validas de usuarios registrados pueden recuperar username despues
  de limpiar el estado local de username en el cliente.
- **SC-003**: 100% de las sesiones guest validas pueden recuperar su tipo de sesion guest sin ser
  representadas como usuarios registrados.
- **SC-004**: Los implementadores frontend pueden identificar la forma documentada de recuperar el
  username actual en menos de 2 minutos leyendo el contrato de auth.
- **SC-005**: La documentacion de contratos no contiene afirmaciones contradictorias sobre ruta de
  profile ni sobre campos username/player ID en su respuesta.

## Assumptions

- Los usuarios registrados tienen usernames inmutables en el sistema actual; las nuevas respuestas
  igualmente usan resolucion del backend para que futuros cambios de identidad visible no requieran
  cambios de contrato frontend.
- Los guests no tienen usernames y deben seguir representandose por display names solo en vistas de
  juego donde aplique.
- El endpoint de identidad de sesion actual sirve para bootstrap de sesion, no para devolver profile
  completo con logros o estadisticas.
- Los clientes existentes que ignoran campos desconocidos siguen siendo compatibles.
