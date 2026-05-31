# Quickstart: Identidad de sesion auth

## Verificacion automatizada

Ejecutar la suite completa:

```powershell
.\gradlew.bat test
```

No ejecutar tests por clase.

## Verificacion manual

1. Levantar la aplicacion:

```powershell
.\gradlew.bat bootRun
```

2. Registrar un usuario:

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/auth/register `
  -ContentType 'application/json' `
  -Body '{"username":"juancho","password":"Clave1!"}'
```

La respuesta debe incluir `username`.

3. Simular reload del FE: conservar solo `accessToken`.

4. Consultar sesion actual:

```powershell
Invoke-RestMethod -Method Get `
  -Uri http://localhost:8080/api/auth/me `
  -Headers @{ Authorization = "Bearer <accessToken>" }
```

La respuesta debe incluir `playerId`, `username` y `tokenUse = user`.

5. Refrescar sesion:

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/auth/refresh `
  -ContentType 'application/json' `
  -Body '{"refreshToken":"<refreshToken>"}'
```

La respuesta debe incluir el nuevo par de tokens y `username`.

6. Crear sesion guest:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/guest
```

Luego consultar `GET /api/auth/me` con ese access token. La respuesta debe tener
`tokenUse = guest` y `username = null`.

## Documentacion

Verificar que `docs/CONTRATOS_API.md` y `README.md` reflejen:

- `username` en register/login/refresh.
- `GET /api/auth/me`.
- JWT sin username, con `sub = playerId`.
- Profile documentado con ruta y shape reales.
- Join pendiente sin sesion valida: conservar `joinCode`/`returnTo`, autenticar por login o
  register,
  y luego ejecutar `POST /api/join/{joinCode}`. Register ya devuelve tokens, sin login posterior.
