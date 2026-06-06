# Data Model — Taxonomía de eventos de aplicación (Fase 1)

Esta feature no introduce entidades persistentes ni cambios de esquema. El "modelo" relevante es la
**clasificación de los application events** según su momento de emisión.

## Marcadores

| Marcador                                | Significado                                            | Comportamiento del publisher                                   |
|-----------------------------------------|--------------------------------------------------------|----------------------------------------------------------------|
| `PostCommitApplicationEvent` (existe)   | Notificación al usuario; se emite tras el commit       | Si hay tx activa, se difiere a `afterCommit`; si no, inmediato |
| `InTransactionApplicationEvent` (NUEVO) | Evento de coordinación que dispara escrituras atómicas | Inmediato (in-tx), igual que cualquier evento no post-commit   |

Regla invariante (enforced por ArchUnit): **toda clase concreta que implemente `ApplicationEvent`
implementa exactamente uno de los dos marcadores.**

## Clasificación de eventos

### Post-commit — notificaciones al usuario

| Evento                            | Contexto  | Estado                                         |
|-----------------------------------|-----------|------------------------------------------------|
| `MatchEventNotification`          | match     | **cambia** → PostCommit (resuelve quick match) |
| `LeagueEventNotification`         | league    | **cambia** → PostCommit                        |
| `CupEventNotification`            | cup       | **cambia** → PostCommit                        |
| `ChatEventNotification`           | chat      | **cambia** → PostCommit                        |
| `SocialEventNotification`         | social    | **cambia** → PostCommit                        |
| `ProfileEventNotification`        | profile   | **cambia** → PostCommit                        |
| `SpectatorMatchEventNotification` | spectator | **cambia** → PostCommit                        |
| `SpectatorCountChanged`           | spectator | **cambia** → PostCommit                        |
| `PresenceEventNotification`       | presence  | ya era PostCommit                              |
| `PublicMatchLobbyNotification`    | lobby     | ya era PostCommit                              |
| `PublicCupLobbyNotification`      | lobby     | ya era PostCommit                              |
| `PublicLeagueLobbyNotification`   | lobby     | ya era PostCommit                              |
| `BotTurnRequired`                 | bot       | ya era PostCommit (necesita estado commiteado) |

### In-transaction — coordinación con escrituras atómicas (categoría C)

| Evento                     | Consumidores que escriben                                                                                                   | Estado                   |
|----------------------------|-----------------------------------------------------------------------------------------------------------------------------|--------------------------|
| `MatchCompleted`           | `LeagueMatchCompletedEventHandler`, `CupMatchCompletedEventHandler`, `SpectatorCleanupOnMatchEndEventHandler`, `ChatMatch*` | **marcar** InTransaction |
| `MatchAbandoned`           | `LeagueMatchAbandonedEventHandler`, `CupMatchAbandonedEventHandler`, `ChatMatchAbandonedEventHandler`                       | **marcar** InTransaction |
| `MatchForfeited`           | `LeagueMatchForfeitedEventHandler`, `CupMatchForfeitedEventHandler`, `ChatMatchForfeitedEventHandler`                       | **marcar** InTransaction |
| `ResourceBecameUnjoinable` | `ResourceUnjoinableInvitationExpirationHandler`                                                                             | **marcar** InTransaction |

> Nota: estos eventos de coordinación, al ejecutarse in-tx, a su vez publican notificaciones
> (`LeagueEventNotification`, `CupEventNotification`, `ChatEventNotification`,
`SpectatorCountChanged`)
> que ahora son post-commit → la escritura queda atómica y la notificación derivada se difiere.

## Transiciones de estado

No aplica (no hay máquinas de estado nuevas). El único "estado" alterado es el momento de emisión,
gobernado por el marcador implementado y la presencia de una transacción activa.
