# Contrato: Stream de presencia en tiempo real

**Feature**: `009-presence-push` | **Fase**: 1 (Design & Contracts)

Define el stream WebSocket/STOMP que empuja los cambios de presencia del **usuario autenticado** a
**todas sus sesiones activas**. Complementa —no reemplaza— a `GET /api/me/presence`
([contrato 008](../../008-user-presence/contracts/presence-api.md)).

## Suscripción

- **Transporte**: WebSocket/STOMP. Conexión: `/ws` (nativo) o `/ws-sockjs` (fallback SockJS).
- **Destino de usuario**: `/user/queue/presence`.
- **Autenticación**: requerida (FR-009). La sesión STOMP debe estar autenticada (JWT) vía el
  interceptor existente; el `Principal` identifica al usuario. Una conexión sin usuario válido no
  recibe notificaciones.
- **Aislamiento**: cada sesión solo recibe notificaciones del **propio** usuario (FR-008).

## Mensaje empujado: `PresenceWsEvent`

`Content-Type: application/json`

Se empuja cuando la ocupación del usuario **cambia** en algún dominio reconectable (entrar a una
partida, arrancar/avanzar una liga o copa, abrir una revancha, o liberarse al finalizar).

| Campo       | Tipo                   | Descripción                                              |
|-------------|------------------------|----------------------------------------------------------|
| `eventType` | string                 | `PRESENCE_UPDATED`.                                      |
| `timestamp` | string (ISO-8601)      | Momento del cambio.                                      |
| `payload`   | `UserPresenceResponse` | Snapshot completo de presencia (mismo shape que la 008). |

### `payload` — `UserPresenceResponse`

Idéntico al body de `200 OK` de `GET /api/me/presence`. Los campos `match`, `league`, `cup`,
`rematch` son objeto cuando hay ocupación o `null` cuando no; `busy` es `true` sii al menos uno es
no-nulo. Ver el [contrato de la 008](../../008-user-presence/contracts/presence-api.md#esquemas)para
los esquemas `ActiveMatchRef`, `ActiveLeagueRef`, `ActiveCupRef`, `ActiveRematchRef`.

**Ejemplo A — el usuario acaba de entrar a una partida de liga en progreso** (una sesión ociosa
recibe esto y deriva a `match.id`):

```json
{
  "eventType": "PRESENCE_UPDATED",
  "timestamp": "2026-06-05T18:24:31.482Z",
  "payload": {
    "busy": true,
    "match": {
      "id": "8f3a1c20-9e44-4b6a-9b2e-2a1f0c7d4e51",
      "status": "IN_PROGRESS"
    },
    "league": {
      "id": "1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed",
      "status": "IN_PROGRESS",
      "currentMatchId": "8f3a1c20-9e44-4b6a-9b2e-2a1f0c7d4e51"
    },
    "cup": null,
    "rematch": null
  }
}
```

**Ejemplo B — el usuario se liberó** (la partida finalizó y no queda nada más; las sesiones vuelven
al home):

```json
{
  "eventType": "PRESENCE_UPDATED",
  "timestamp": "2026-06-05T18:51:09.120Z",
  "payload": {
    "busy": false,
    "match": null,
    "league": null,
    "cup": null,
    "rematch": null
  }
}
```

## Garantías

- **Entrega a todas las sesiones del dueño**: cada sesión activa autenticada del usuario recibe la
  misma notificación, incluida la que originó el cambio (FR-001 / FR-002).
- **Aislamiento por usuario** (FR-008): la notificación nunca llega a sesiones de terceros.
- **Coherencia** (FR-013): si la partida pertenece a un torneo en progreso,
  `payload.match.id == payload.league.currentMatchId` (o `cup.currentMatchId`).
- **Solo lectura** (FR-010): emitir la notificación no altera partidas, ligas, copas, revanchas ni
  sus
  temporizadores de inactividad.
- **Best-effort** (FR-011): la entrega no es exactamente-una-vez. Si una sesión pierde un mensaje (o
  se conecta después del cambio), reconcilia su estado con `GET /api/me/presence`. El stream cubre
  los
  cambios **posteriores** a la suscripción; el arranque en frío se resuelve con la consulta pull.
- **Bots y quick match** excluidos: los bots no reciben notificaciones; la cola de quick match no
  genera notificación (sí la partida resultante).

## Relación con la 008 (pull)

| Aspecto           | `GET /api/me/presence` (008)      | `/user/queue/presence` (009)                  |
|-------------------|-----------------------------------|-----------------------------------------------|
| Modelo            | Pull (snapshot puntual)           | Push (snapshot ante cada cambio)              |
| Cuándo            | Al cargar / reconectar (arranque) | Mientras la sesión está suscripta             |
| Shape del payload | `UserPresenceResponse`            | `UserPresenceResponse` (idéntico) + metadatos |
| Rol               | Fuente de verdad / reconciliación | Sincronización en vivo entre sesiones         |
