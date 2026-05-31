# Contracts: Identidad de sesion auth

## Register

`POST /api/auth/register`

Response `200`:

```json
{
  "playerId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "username": "juancho",
  "accessToken": "<jwt>",
  "refreshToken": "<opaque-refresh-token>",
  "accessTokenExpiresIn": 900,
  "refreshTokenExpiresIn": 2592000
}
```

## Login

`POST /api/auth/login`

Response `200`:

```json
{
  "playerId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "username": "juancho",
  "accessToken": "<jwt>",
  "refreshToken": "<opaque-refresh-token>",
  "accessTokenExpiresIn": 900,
  "refreshTokenExpiresIn": 2592000
}
```

## Refresh

`POST /api/auth/refresh`

Response `200`:

```json
{
  "playerId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "username": "juancho",
  "accessToken": "<jwt>",
  "refreshToken": "<new-opaque-refresh-token>",
  "accessTokenExpiresIn": 900,
  "refreshTokenExpiresIn": 2592000
}
```

## Current Session

`GET /api/auth/me`

Auth: Bearer requerido.

Response `200` para usuario registrado:

```json
{
  "playerId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "username": "juancho",
  "tokenUse": "user"
}
```

Response `200` para guest:

```json
{
  "playerId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "username": null,
  "tokenUse": "guest"
}
```

Errores:

- `401` si falta Bearer token, el token es invalido o expiro.
- `401` si el token dice `tokenUse = user` pero el usuario registrado ya no se puede resolver.

## JWT

Sin cambios:

- `sub`: `playerId` (UUID)
- `token_use`: `user` o `guest`

El JWT no incorpora `username`.
