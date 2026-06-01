# Modelo de Datos: Catálogo de Logros

Esta feature **no introduce nuevas estructuras persistidas**. El catálogo es una proyección del enum
de dominio existente. Se describen las entidades conceptuales y los DTOs de transporte.

## Entidades de dominio

### AchievementCode (enum, YA EXISTE)

- **Qué representa**: el conjunto cerrado de logros que existen en el juego. Cada constante es un
  logro con un código estable.
- **Atributos**: solo el nombre de la constante (el código). Sin título, sin descripción, sin flag
  de oculto.
- **Fuente de verdad**: este enum ES el catálogo. No se agrega ningún campo en esta feature.
- **Invariantes**: los códigos no se eliminan ni renombran una vez publicados (supuesto de la spec).

## DTOs de aplicación

### AchievementCatalogDTO (NUEVO)

- **Qué representa**: el catálogo completo devuelto por el caso de uso de consulta.
- **Forma**: lista de `AchievementCode` (los valores del enum).
- **Reglas**: refleja exactamente `AchievementCode.values()`, en el orden de declaración del enum.
  Sin filtrado (no hay ocultos) y sin estado por jugador.

## DTOs de respuesta (HTTP)

### AchievementCatalogResponse (NUEVO)

```text
AchievementCatalogResponse {
  achievements: AchievementCatalogItemResponse[]
}

AchievementCatalogItemResponse {
  achievementCode: string   // p. ej. "WIN_GAME_THREE_ZERO_VIA_ACCEPTED_RETRUCO"
}
```

- **Mapeo**: `AchievementCatalogResponse.from(AchievementCatalogDTO)` convierte cada
  `AchievementCode` en un item con `achievementCode = code.name()`.
- **Naming**: el campo `achievementCode` coincide con `UnlockedAchievementResponse.achievementCode`
  del perfil, para que el frontend mergee por esa clave.

## Relación con el perfil (sin cambios)

El recurso de perfil sigue igual: `PlayerProfileResponse.achievements[]` son
`UnlockedAchievementResponse { achievementCode, unlockedAt, matchId, gameNumber }`, solo los
desbloqueados. El frontend cruza:

```text
catálogo.achievements  ⨝(achievementCode)  perfil.achievements  →  vista "todos con flag unlocked"
```

## Validaciones

- El catálogo no recibe input del cliente: no hay validación de request.
- Acceso: requiere usuario autenticado (Bearer JWT), igual criterio que el perfil.
