# Quickstart: Catálogo de Logros

## Qué se construye

Un endpoint `GET /api/achievements` que lista todos los logros existentes (sus códigos). El frontend
lo combina con `GET /api/profile/{username}` para mostrar la grilla "todos los logros con marca de
desbloqueado".

## Probar localmente

```bash
# Levantar dependencias y la app
docker compose up -d
./gradlew bootRun

# Obtener un JWT (registro/login según el flujo de auth existente) y consultar el catálogo
curl -H "Authorization: Bearer <jwt>" http://localhost:8080/api/achievements
```

Respuesta esperada: un objeto con `achievements`, una entrada por cada logro existente, cada una con
su `achievementCode`.

También visible en Swagger: `http://localhost:8080/swagger-ui/index.html`, tag de logros.

## Cómo lo usa el frontend

```text
catálogo  = GET /api/achievements        # qué logros existen (cacheable)
perfil    = GET /api/profile/{username}  # qué desbloqueó el jugador

grilla = catálogo.achievements.map(c => ({
  achievementCode: c.achievementCode,
  title:        textos[c.achievementCode].title,        // resuelto en el FE
  description:  textos[c.achievementCode].description,   // resuelto en el FE
  unlocked:     perfil.achievements.some(u => u.achievementCode === c.achievementCode),
}))
```

## Verificación (tests)

```bash
# Test del handler de catálogo
./gradlew test --tests "com.villo.truco.profile.application.usecases.queries.GetAchievementCatalogQueryHandlerTest"

# Test del controller (slice MVC)
./gradlew test --tests "com.villo.truco.profile.infrastructure.http.AchievementControllerTest"

# Test de contrato: enum AchievementCode ↔ docs/CONTRATOS_API.md §8.3
./gradlew test --tests "com.villo.truco.profile.infrastructure.http.AchievementCatalogContractTest"

# Suite completa + coverage
./gradlew build
```

## Criterios de aceptación cubiertos

- Un jugador sin logros ve el 100% del catálogo (SC-001).
- El frontend no necesita lista de logros hardcodeada (SC-002).
- Agregar un logro al enum lo expone automáticamente, sin migraciones (SC-003).
- Agregar un código sin documentarlo en §8.3 rompe el build vía el test de contrato (SC-004).
