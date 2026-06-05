# Quickstart: Presencia en tiempo real (push de ocupación)

**Feature**: `009-presence-push`

Guía rápida para probar manualmente el push de presencia entre múltiples sesiones del mismo usuario.

## Requisitos

- Dependencias locales arriba: `docker compose up -d` (PostgreSQL + Adminer).
- App corriendo: `./gradlew bootRun`.
- Un JWT válido del **mismo** usuario para abrir dos sesiones (dos pestañas / dos clientes STOMP).
- Cliente de prueba: `src/main/resources/WebSocketTest.html` o cualquier cliente STOMP.

## Escenario P1 — la sesión ociosa se entera de la partida

1. **Sesión A** y **Sesión B**: conectar por STOMP a `/ws` autenticando con el JWT del usuario y
   suscribirse a `/user/queue/presence`.
2. (Opcional) En ambas, hacer `GET /api/me/presence` → debe devolver `busy: false` (arranque en
   frío).
3. **Sesión A**: crear o unirse a una partida (p. ej. `POST /api/matches` y unirse, o vía quick
   match).
4. **Esperado**: **ambas** sesiones reciben un `PresenceWsEvent` con `eventType: PRESENCE_UPDATED` y
   `payload.match.id` igual al id de la partida. La **Sesión B** (ociosa) usa ese id para derivar a
   la
   partida.
5. Verificar que `GET /api/me/presence` ahora devuelve el mismo `match` (push y pull coinciden).

## Escenario P2 — arranque de liga/copa

1. Dos sesiones del usuario suscriptas a `/user/queue/presence`.
2. Inscribir al usuario en una liga y completar/iniciar el torneo (que dispare `LeagueStarted` /
   `LeagueMatchActivated`).
3. **Esperado**: ambas sesiones reciben un snapshot con `league.status: IN_PROGRESS` y
   `league.currentMatchId` = id de la partida del torneo, coherente con `match.id`.

## Escenario P3 — revancha

1. Dos sesiones suscriptas.
2. Finalizar una partida elegible para revancha (dispara `RematchSessionOpened`).
3. **Esperado**: ambas sesiones reciben un snapshot con `rematch.id` y `rematch.originMatchId`.

## Escenario P4 — liberación

1. Usuario ocupado en una partida, con sesiones suscriptas.
2. Finalizar la partida (`MatchFinished`).
3. **Esperado**: ambas sesiones reciben un snapshot con `match: null`; si no queda otra ocupación,
   `busy: false`. Las sesiones vuelven al home.

## Verificación de garantías

- **Aislamiento**: con un **segundo** usuario suscripto a su propia `/user/queue/presence`,
  confirmar
  que **no** recibe las notificaciones del primero.
- **Solo lectura**: invocar/recibir notificaciones no debe alterar el estado ni reiniciar
  temporizadores de inactividad de partidas/ligas/copas/revanchas (comparar antes/después).
- **Reconciliación**: desconectar la Sesión B durante un cambio y reconectar → al re-suscribirse,
  obtiene el estado correcto con `GET /api/me/presence` (el stream solo trae cambios posteriores).

## Tests automatizados

- `*PresenceEventTranslatorTest` (4): por cada evento de transición, verifican que se publica un
  `PresenceEventNotification` por jugador humano (bots filtrados) con el snapshot resuelto.
- `UserPresenceResolverTest`: la resolución coincide con la de la 008 (puede reusarse el test del
  handler 008 tras el refactor).
- `StompPresenceNotificationHandlerTest`: integración STOMP — el snapshot llega a
  `/user/queue/presence` del destinatario y no a otros.
