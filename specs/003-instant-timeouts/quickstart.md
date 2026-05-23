# Quickstart: Verificación local de timeouts instantáneos

## Preparación

```powershell
docker compose up -d
./gradlew bootRun
```

Para facilitar la verificación, configurar plazos cortos vía variables de entorno o
`application-local.yaml`:

```yaml
truco:
  match:
    idle-timeout-seconds: 10
  cup:
    idle-timeout-seconds: 15
  league:
    idle-timeout-seconds: 15
  social:
    invitation-expiration-seconds: 20
  rematch:
    session-expiration-seconds: 20
```

## Caso 1: Match — timeout exacto

1. Crear una partida con dos jugadores autenticados (usar Swagger o el frontend local).
2. Dejar la partida sin acciones.
3. Marcar el reloj cuando se realice la última acción.
4. Observar el log: debe aparecer `Timeout programado: entityType=MATCH, ..., etaSeconds≈10`
   tras la acción.
5. A los 10 s exactos (±1 s) debe aparecer `Timeout disparado: entityType=MATCH, ..., lagMs<1000`
   y la partida queda en estado terminal.

**Criterio de éxito**: el evento WebSocket de fin de partida llega a los clientes en t≈10 s,
no en t≈30–40 s como antes.

## Caso 2: Acción que reinicia el deadline

1. Crear partida. Esperar 7 s.
2. Realizar una acción válida (jugar una carta).
3. En el log debe aparecer un nuevo `Timeout programado` con `etaSeconds≈10` (reseteado).
4. La partida NO debe finalizar a los 10 s desde la creación; debe finalizar 10 s después de la
   acción nueva.

## Caso 3: Cierre natural cancela el timeout

1. Crear partida.
2. Llevarla a fin por puntaje normalmente antes de que vence el timeout.
3. En el log debe aparecer `Timeout cancelado: entityType=MATCH, ..., razón=MatchFinalized`.
4. La métrica `truco.timeout.fired{entityType=MATCH,outcome=skipped}` no debe aumentar para esa
   partida (porque ni siquiera se disparó).

## Caso 4: Reinicio del servicio

1. Crear partida con `idle-timeout-seconds=60`.
2. Esperar 20 s.
3. Detener el servicio (`Ctrl+C`).
4. Esperar 50 s (con esto el deadline ya venció).
5. Levantar el servicio nuevamente.
6. En el log debe aparecer `Timeout reconciliado al arrancar: ..., vencidos=1, ...` y de
   inmediato `Timeout disparado: ..., lagMs<10000`.

## Caso 5: Copa / Liga / Rematch / Invitación social

Repetir el patrón de los casos 1–3 sobre cada uno de los otros bounded contexts. El log debe
mostrar la misma secuencia de eventos con
`entityType=CUP|LEAGUE|REMATCH_SESSION|RESOURCE_INVITATION`.

## Métricas

Abrir Actuator:

```
http://localhost:8080/actuator/metrics/truco.timeout.lag
http://localhost:8080/actuator/metrics/truco.timeout.pending
```

`truco.timeout.lag` debe mostrar percentil 99 sub-segundo para todas las muestras del experimento.

## Tests automatizados

```powershell
./gradlew test --tests "com.villo.truco.infrastructure.scheduler.*"
./gradlew test --tests "com.villo.truco.application.eventhandlers.TimeoutSchedulingEventHandlerTest"
./gradlew test --tests "com.villo.truco.integration.TimeoutExactnessIT"
```

Y la suite completa:

```powershell
./gradlew build
```

`build` ejecuta también la verificación de cobertura (70 % mínimo) y el suite de ArchUnit.
