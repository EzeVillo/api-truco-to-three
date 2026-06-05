# Quickstart — Verificar las notificaciones post-commit

## Objetivo

Confirmar que las notificaciones se emiten sólo tras el commit (categoría A) y que no hay avisos
fantasma ni duplicados (categoría B), sin romper el comportamiento existente.

## Verificación automatizada

```bash
# Suite completa (incluye ArchUnit + cobertura ≥ 70%)
./gradlew build

# Regla de arquitectura de la salvaguarda
./gradlew test --tests "*CleanArchitectureTest"

# Timing del publisher (post-commit / rollback / sin tx)
./gradlew test --tests "*TransactionalApplicationEventPublisherTest"
```

### Qué deben cubrir los tests

- **Categoría A (quick match)**: al emparejarse dos jugadores, la notificación de partida encontrada
  se entrega sólo después del commit; la consulta inmediata del match lo encuentra.
- **Categoría B (chat)**: ante rollback de la operación, no se emite notificación; ante reintento
  que
  confirma, se emite exactamente una vez.
- **Salvaguarda**: agregar mentalmente (o en un test fixture) un `ApplicationEvent` sin marcador
  hace
  fallar la regla ArchUnit.

## Verificación manual (opcional)

1. Levantar dependencias y la app:
   ```bash
   docker compose up -d
   ./gradlew bootRun
   ```
2. Con dos clientes en la cola de partida rápida, provocar el emparejamiento.
3. En el cliente que recibe la notificación, consultar el match por su id **inmediatamente**.
4. **Esperado**: la partida existe y carga (sin 404), sin necesidad de refrescar.

## Criterio de aceptación

- `./gradlew build` pasa (tests + ArchUnit + cobertura).
- Cero 404 inmediatos tras notificación de recurso recién creado en los flujos del alcance.
- Sin notificaciones fantasma/duplicadas bajo rollback/retry en chat.
