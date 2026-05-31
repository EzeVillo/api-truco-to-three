# Research: Identidad de sesion auth

## Decision 1: No agregar username al JWT

**Decision**: Mantener `sub = playerId` y `token_use = user | guest`; resolver `username` desde el
backend al armar respuestas y al consultar la sesion actual.

**Rationale**: El contrato actual ya declara `sub` como identificador tecnico (`playerId`). Cambiar
el JWT ampliaria el blast radius en seguridad, WebSocket y consumidores existentes. Resolver desde
backend evita datos stale si en el futuro cambia la identidad visible.

**Alternatives considered**:

- Agregar claim `username`: mas comodo para el FE, pero introduce identidad visible en un token que
  puede vivir hasta expirar y aumenta acoplamiento.
- Cambiar `sub` a username: incompatible con el contrato y con autorizacion por player ID.

## Decision 2: Exponer `GET /api/auth/me`

**Decision**: Agregar un endpoint protegido de bootstrap de sesion en auth: `GET /api/auth/me`.

**Rationale**: El problema principal aparece en sesiones existentes tras recargar. El FE puede tener
tokens validos pero no username local. Un endpoint `me` es una convencion simple y evita depender de
profile, que requiere conocer username antes de consultar.

**Alternatives considered**:

- Usar `GET /api/profile/{username}`: circular, porque requiere username para obtener username.
- Agregar `GET /api/profile/me`: mezcla bootstrap de sesion con profile completo; mas alcance del
  necesario.
- Persistir username solo en FE: incompleto para sesiones existentes y puede quedar stale.

## Decision 3: Incluir username en register/login/refresh

**Decision**: `register`, `login` y `refresh` para usuarios registrados devuelven `username` junto a
`playerId`, tokens y expiraciones.

**Rationale**: Inicializa el estado de sesion con la fuente autoritativa y evita una llamada extra
despues de auth. En refresh tambien corrige sesiones donde el FE no tiene username o tiene uno
viejo.

**Alternatives considered**:

- Solo `GET /api/auth/me`: suficiente para rehidratar, pero obliga a llamada extra despues de cada
  login/register si el FE quiere una fuente autoritativa inmediata.
- Solo auth responses: no cubre sesiones ya existentes tras reload sin username local.

## Decision 4: Guests sin username explicito

**Decision**: `GET /api/auth/me` devuelve `tokenUse = guest` y `username = null` para guests. La
respuesta de guest login se mantiene sin username.

**Rationale**: Guest no persiste cuenta y no tiene username. Inventar uno en auth confundiria
identidad registrada con display name de presentacion. Las vistas de juego ya resuelven
`displayName` donde aplica.

**Alternatives considered**:

- Devolver un display name guest en auth: aumenta alcance y mezcla presentacion con auth.
- Crear username efimero: contradice que guest no persiste cuenta.

## Decision 5: Resolver username de refresh desde repositorio de usuarios

**Decision**: Al refrescar una sesion registrada, el caso de uso resuelve el username del `playerId`
asociado al refresh token antes de construir la respuesta.

**Rationale**: El refresh token opaco identifica una sesion de usuario pero no transporta username.
La fuente autoritativa es el repositorio de usuarios.

**Alternatives considered**:

- Guardar username en la entidad de refresh session: redundante y puede quedar stale.
- Devolver username desde el token provider: rompe separacion; el provider emite tokens, no conoce
  usuarios.

## Decision 6: Documentar profile segun comportamiento real

**Decision**: Corregir documentacion de profile para que no prometa username en la respuesta si el
DTO actual no lo devuelve, y para aclarar que la busqueda actual es case-insensitive.

**Rationale**: La feature toca identidad visible y contratos de FE. Mantener contradicciones en
profile haria que el FE tome decisiones incorrectas.

**Alternatives considered**:

- Cambiar profile para devolver username: es razonable, pero no es necesario para resolver auth
  bootstrap y ampliaria el alcance.
