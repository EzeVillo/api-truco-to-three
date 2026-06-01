# Data Model: Identidad de sesion auth

## UserAuthenticatedSession

Representa una sesion autenticada de usuario registrado devuelta por register, login y refresh.

**Campos**:

- `playerId`: UUID tecnico del jugador registrado.
- `username`: username autoritativo del usuario registrado.
- `accessToken`: JWT Bearer para endpoints protegidos.
- `accessTokenExpiresIn`: segundos hasta expiracion del access token.
- `refreshToken`: token opaco rotado para sesiones registradas.
- `refreshTokenExpiresIn`: segundos hasta expiracion del refresh token.

**Reglas**:

- `username` es obligatorio para sesiones registradas.
- `playerId` sigue siendo el identificador tecnico usado por el JWT.
- El refresh token rota como hasta ahora.

## GuestAuthenticatedSession

Representa una sesion guest efimera.

**Campos**:

- `playerId`: UUID efimero.
- `accessToken`: JWT Bearer de guest.
- `accessTokenExpiresIn`: segundos hasta expiracion del access token.

**Reglas**:

- No tiene `username`.
- No tiene `refreshToken`.
- `token_use` del JWT es `guest`.

## AuthenticatedSessionIdentity

Representa la identidad actual del caller obtenida desde un access token valido.

**Campos**:

- `playerId`: UUID del subject del token.
- `tokenUse`: `user` o `guest`.
- `username`: username cuando `tokenUse = user`; `null` cuando `tokenUse = guest`.

**Reglas**:

- Si `tokenUse = user`, `username` debe resolverse desde backend.
- Si `tokenUse = guest`, `username` debe ser `null`.
- Si el token es invalido, expirado o no tiene subject usable, no hay identidad.

## Relaciones

- `UserAuthenticatedSession` se construye desde un `User` registrado y una `UserSession`.
- `AuthenticatedSessionIdentity` para usuario registrado depende de resolver `playerId -> username`.
- `GuestAuthenticatedSession` y `AuthenticatedSessionIdentity` guest no dependen de tabla de
  usuarios.

## Validaciones

- `playerId` debe ser UUID valido.
- `tokenUse` solo acepta `user` o `guest`.
- Para usuarios registrados, la ausencia de username es un error de sesion/identidad, no una
  respuesta parcial.
