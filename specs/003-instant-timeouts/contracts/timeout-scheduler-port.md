# Contrato: Puerto `TimeoutScheduler`

**Ubicación**: `com.villo.truco.application.ports.out.TimeoutScheduler`

**Tipo**: Puerto de salida (outbound port) del módulo de aplicación. Implementación viva en
`infrastructure/scheduler/SpringTimeoutScheduler`.

## Interfaz (forma esperada en código)

```java
public interface TimeoutScheduler {

  void schedule(TimeoutKey key, Instant deadline, Runnable action);

  void cancel(TimeoutKey key);

  boolean isPending(TimeoutKey key);
}
```

`TimeoutKey` es un value object inmutable con dos campos: `EntityType` (enum: `MATCH`, `CUP`,
`LEAGUE`, `REMATCH_SESSION`, `RESOURCE_INVITATION`) y `String entityId` (la representación textual
del UUID/identificador del agregado).

## Semántica

### schedule(key, deadline, action)

- Si ya hay un future para `key`, lo cancela primero.
- Si `deadline <= now()`, ejecuta `action` inmediatamente en el pool (no en el thread que llama).
- Si `deadline > now()`, agenda `action` para ejecutarse en `deadline`.
- `action` se ejecuta a lo sumo una vez por programación.
- Si la JVM se reinicia antes del disparo, el future se pierde — la reconciliación al arranque lo
  restaura.

### cancel(key)

- Si hay un future agendado para `key`, lo cancela y lo elimina del mapa.
- Si no hay nada, no-op.
- Es seguro llamar después del disparo (no-op).

### isPending(key)

- `true` si hay un future agendado y no disparado para `key`.
- Pensado sólo para observabilidad/tests.

## Contrato de exactitud

- **Latencia de disparo**: percentil 99 ≤ 1000 ms desde `deadline` bajo carga normal.
- **Latencia máxima** observada durante operación normal (sin restart): ≤ 3 s.
- **Sin disparo duplicado**: para una misma `(key, schedule call)`, `action` se ejecuta exactamente
  una vez (la cancelación tras disparar es no-op).

## Quién llama a quién

- **Productores de `schedule`/`cancel`**:
    - `TimeoutSchedulingEventHandler` (en `application/eventhandlers`), suscrito a eventos de
      dominio que reinician o terminan el timeout.
    - `TimeoutReconciliationRunner` al arrancar y desde la red de seguridad cada 5 min.
- **Productor de `action`**:
    - Una lambda construida por el caller que invoca el `*UseCase.handle(entityId)` apropiado
      envuelto en `RetryableTransactionalRunner`.
- **Consumidores de `isPending`**: tests y, opcionalmente, un endpoint de actuator para
  diagnóstico.

## No se expone HTTP

Este puerto no se expone vía REST ni WebSocket. No hay cambios en `docs/CONTRATOS_API.md`
salvo la eventual aclaración de que los timeouts ahora son instantáneos (ver "Documentación a
actualizar" en spec).
