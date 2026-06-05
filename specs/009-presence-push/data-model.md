# Data Model: Presencia en tiempo real (push de ocupación)

**Feature**: `009-presence-push` | **Fase**: 1 (Design & Contracts)

Esta feature **no introduce entidades persistentes** (sin tablas ni migraciones). Modela un evento
de
aplicación en tránsito y reutiliza los DTOs de presencia de la 008. Se documentan las estructuras en
memoria y el mapeo evento-de-dominio → notificación.

## Estructuras en memoria

### `PresenceEventNotification` (application event — ✚ nuevo)

Evento de aplicación que viaja del traductor de presencia al handler STOMP.

| Campo       | Tipo              | Descripción                                                          |
|-------------|-------------------|----------------------------------------------------------------------|
| `recipient` | `PlayerId`        | Único destinatario (el dueño del estado). Aislamiento por usuario.   |
| `eventType` | `String`          | Identificador del cambio, p. ej. `PRESENCE_UPDATED`.                 |
| `timestamp` | `Instant`         | Momento del cambio.                                                  |
| `snapshot`  | `UserPresenceDTO` | Snapshot de presencia del destinatario (reutiliza el DTO de la 008). |

> A diferencia de `SocialEventNotification` (que lleva una lista de `recipients` y un payload
> común),
> aquí el payload es **por usuario**, por lo que el destinatario es único y se emite una
> notificación
> por jugador humano afectado.

### `UserPresenceDTO` y sub-refs (♻ reutilizados de la 008)

Sin cambios. Se reutilizan tal cual:
[`UserPresenceDTO`](../../src/main/java/com/villo/truco/application/dto/UserPresenceDTO.java)
(`busy`, `match`, `league`, `cup`, `rematch`) y `ActiveMatchRefDTO` / `ActiveLeagueRefDTO` /
`ActiveCupRefDTO` / `ActiveRematchRefDTO`.

### `PresenceWsEvent` (infrastructure DTO — ✚ nuevo)

Forma serializada que viaja al cliente por STOMP (análogo a `SocialWsEvent`).

| Campo       | Tipo                   | Descripción                                   |
|-------------|------------------------|-----------------------------------------------|
| `eventType` | `String`               | p. ej. `PRESENCE_UPDATED`.                    |
| `timestamp` | `Instant`              | Momento del cambio.                           |
| `payload`   | `UserPresenceResponse` | Snapshot (mismo shape que el body de la 008). |

## Componentes (sin estado persistente)

### `UserPresenceResolver` (application — ✚ extraído de la 008)

- **Responsabilidad**: dado un `PlayerId`, resolver su `UserPresenceDTO` agregando las lecturas de
  `MatchQueryRepository`, `LeagueQueryRepository`, `CupQueryRepository`, `RematchSessionRepository`
  (la lógica que hoy vive en `GetUserPresenceQueryHandler`).
- **Reglas** (idénticas a la 008): `currentMatchId` de liga/copa solo si el torneo está
  `IN_PROGRESS`;
  coherencia `match.id == league/cup.currentMatchId`; `busy = ` alguna ref no nula.
- **Solo lectura**: no escribe ni reinicia temporizadores.

### Traductores de presencia por dominio (application — ✚ nuevos)

Implementan los puertos `*DomainEventHandler` existentes; se enchufan en los composites de
notificadores. Para cada evento relevante: extraen los jugadores afectados → filtran bots
(`BotRegistry`) → por cada jugador humano, `snapshot = resolver.resolve(player)` →
`applicationEventPublisher.publish(new PresenceEventNotification(player, ...snapshot))`.

| Traductor                        | Puerto que implementa                         | Eventos que filtra (ver research §4) |
|----------------------------------|-----------------------------------------------|--------------------------------------|
| `MatchPresenceEventTranslator`   | `MatchDomainEventHandler<MatchDomainEvent>`   | entrar/liberar partida               |
| `LeaguePresenceEventTranslator`  | `LeagueDomainEventHandler<LeagueDomainEvent>` | entrar/arrancar/avanzar/liberar liga |
| `CupPresenceEventTranslator`     | `CupDomainEventHandler<CupDomainEvent>`       | entrar/arrancar/avanzar/liberar copa |
| `RematchPresenceEventTranslator` | `RematchSessionDomainEventHandler<...>`       | abrir/cerrar revancha                |

### `StompPresenceNotificationHandler` (infrastructure — ✚ nuevo)

- Implementa `ApplicationEventHandler<PresenceEventNotification>`.
- Mapea `snapshot` (DTO) → `UserPresenceResponse` y empuja un `PresenceWsEvent` a
  `/user/queue/presence` del `recipient` vía `SimpMessagingTemplate.convertAndSendToUser`.
- Registra éxito/fallo en `EventNotifierHealthRegistry`.

## Mapeo dominio → notificación (transiciones de ocupación)

```text
Domain event (match/league/cup/rematch)
        │  (despachado tras save, misma tx)
        ▼
*PresenceEventTranslator
        │  jugadores afectados − bots
        ▼
para cada jugador humano:
   snapshot = UserPresenceResolver.resolve(player)        ← lecturas (mismas que la 008)
   publish PresenceEventNotification(player, snapshot)
        ▼
StompPresenceNotificationHandler
        ▼
SimpMessagingTemplate → /user/queue/presence  (solo el dueño)
```

## Validaciones / invariantes

- **Aislamiento (FR-008)**: `recipient` es siempre el dueño del `snapshot`; nunca se envía el estado
  de un usuario a la cola de otro.
- **Coherencia (FR-013)**: garantizada porque el snapshot proviene del mismo `UserPresenceResolver`
  que la 008.
- **Solo lectura (FR-010)**: los traductores y el resolver solo consultan repos de lectura.
- **Bots**: nunca generan ni reciben notificaciones de presencia.
- **Quick match (FR-012)**: la cola de quick match no es un evento de presencia; solo la partida ya
  creada (vía `PlayerJoinedEvent` / `MatchDerivedEvent`) dispara notificación.
