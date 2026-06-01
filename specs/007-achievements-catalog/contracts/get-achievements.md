# Contrato: GET /api/achievements

## Resumen

Devuelve el catálogo completo de logros existentes en el juego (sus códigos estables). Es
independiente del jugador: la respuesta es idéntica para cualquier usuario. No incluye título ni
descripción (los resuelve el frontend) ni estado de desbloqueo (eso vive en el perfil).

## Request

```
GET /api/achievements
Authorization: Bearer <jwt>
```

- **Body**: ninguno.
- **Path/Query params**: ninguno.
- **Auth**: requiere JWT válido (cualquier usuario autenticado), igual criterio que
  `GET /api/profile/{username}`.

## Response 200 — AchievementCatalogResponse

```json
{
  "achievements": [
    { "achievementCode": "WIN_GAME_AS_PIE_MANO_BUSTS_ON_ENVIDO_WITH_0_0_AT_2_2" },
    { "achievementCode": "WIN_GAME_AS_MANO_VIA_FALTA_ENVIDO_WITH_33_33_AT_2_2" },
    { "achievementCode": "WIN_GAME_BUST_OPPONENT_VIA_QUIERO_Y_ME_VOY_AL_MAZO" },
    { "achievementCode": "WIN_HAND_UNCONTESTED_WITH_ANCHO_DE_ESPADA" },
    { "achievementCode": "FOLD_BEFORE_ANY_CARD_IS_PLAYED" },
    { "achievementCode": "WIN_GAME_THREE_ZERO_VIA_ACCEPTED_RETRUCO" },
    { "achievementCode": "WIN_GAME_THREE_ZERO_VIA_REAL_OR_FALTA_ENVIDO" },
    { "achievementCode": "WIN_GAME_FROM_2_2_WITHOUT_CALLS_IN_ROUND" },
    { "achievementCode": "WIN_GAME_BUST_OPPONENT_VIA_VALE_CUATRO_LOSS_AT_0_0" },
    { "achievementCode": "WIN_GAME_BUST_RIVAL_VIA_FOLD_AFTER_ACCEPTED_TRUCO_WITH_NO_CARDS" }
  ]
}
```

- `achievements`: lista de todos los logros existentes. Cantidad = cantidad de valores de
  `AchievementCode`. Orden = orden de declaración del enum.
- `achievements[].achievementCode`: código estable del logro. Mismo nombre de campo y mismos valores
  posibles que `PlayerProfileResponse.achievements[].achievementCode`.

## Response 401 — No autenticado

Token ausente o inválido. Cuerpo: `ErrorResponse` (formato estándar del proyecto).

## Reglas verificables

1. La lista contiene exactamente los códigos de `AchievementCode`, sin omisiones ni duplicados.
2. Para un jugador sin logros desbloqueados, la respuesta es idéntica (catálogo completo).
3. El conjunto de `achievementCode` devuelto coincide con la sección §8.3 de
   `docs/CONTRATOS_API.md` (verificado por el test de contrato).
4. La respuesta no contiene campos de título, descripción, ni estado de desbloqueo.
