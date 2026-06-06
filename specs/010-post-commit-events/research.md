# Research — Notificaciones post-commit (Fase 0)

Todas las incógnitas marcadas en la spec quedaron resueltas mediante inspección del código. No
quedan
`NEEDS CLARIFICATION`.

## Decisión 1 — Mecanismo de emisión post-commit

- **Decisión**: Reutilizar `PostCommitApplicationEvent` + `TransactionalApplicationEventPublisher`.
- **Rationale**: El publisher (`infrastructure/events/TransactionalApplicationEventPublisher.java`)
  ya inspecciona si el evento implementa `PostCommitApplicationEvent` y, si hay sincronización de
  transacción activa, registra una `TransactionSynchronization` que publica en `afterCommit`. Si no
  hay tx activa, publica inmediato. Es exactamente el comportamiento deseado y ya está probado.
- **Alternativas consideradas**:
    - Invertir el default (todo post-commit salvo opt-in in-tx): mayor cambio semántico y más
      riesgo;
      descartado por YAGNI. La salvaguarda con marcador explícito logra el mismo objetivo de "no
      olvidarse" sin invertir el comportamiento del publisher.
    - Usar `@TransactionalEventListener(phase = AFTER_COMMIT)` de Spring: implicaría meter Spring en
      la
      capa application (viola Principio II) o reescribir el pipeline de eventos. Descartado.

## Decisión 2 — Publisher único y compartido entre bounded contexts

- **Decisión**: Confiar en el bean `@Primary ApplicationEventPublisher` único.
- **Rationale**: `ApplicationEventConfiguration` define un único bean `@Primary` que es el
  `TransactionalApplicationEventPublisher` envolviendo al `InProcessApplicationEventPublisher`. Los
  translators de social (`SocialApplicationEventConfiguration`) y profile
  (`ProfileApplicationEventConfiguration`) reciben `ApplicationEventPublisher` por inyección → usan
  el
  mismo bean. Por lo tanto, marcar `SocialEventNotification` y `ProfileEventNotification` como
  post-commit funciona sin wiring adicional.
- **Alternativas consideradas**: ninguna; verificado por lectura de las tres configuraciones.

## Decisión 3 — Qué eventos van post-commit y cuáles in-transaction

- **Decisión**:
    - **Post-commit** (notificaciones al usuario): `MatchEventNotification`,
      `LeagueEventNotification`,
      `CupEventNotification`, `ChatEventNotification`, `SocialEventNotification`,
      `ProfileEventNotification`, `SpectatorMatchEventNotification`, `SpectatorCountChanged`.
    - **In-transaction** (coordinación con escrituras atómicas): `MatchCompleted`, `MatchAbandoned`,
      `MatchForfeited`, `ResourceBecameUnjoinable`.
- **Rationale**:
    - Las notificaciones son consumidas por handlers STOMP que sólo hacen `convertAndSend` (no
      escriben). Diferirlas elimina el 404 (A) y el fantasma/duplicado (B).
    - `MatchCompleted/Abandoned/Forfeited` son consumidos por `LeagueMatch*EventHandler`,
      `CupMatch*EventHandler` (avance de competición = escrituras),
      `SpectatorCleanupOnMatchEndEventHandler`
      (escritura) y `ChatMatch*EventHandler` (escritura). `ResourceBecameUnjoinable` es consumido
      por
      `ResourceUnjoinableInvitationExpirationHandler` (expira invitaciones = escritura). Moverlos a
      post-commit partiría la atomicidad → quedan in-tx.
- **Alternativas consideradas**: regla por convención de nombre (sufijo `Notification` →
  post-commit).
  Descartada porque `SpectatorCountChanged` no termina en `Notification` y quedaría fuera; el
  marcador
  explícito es inequívoco.

## Decisión 4 — Contexto transaccional de espectadores y perfil (punto abierto de la spec)

- **Decisión**: Tratarlos como post-commit (corren dentro de tx de escritura).
- **Rationale**:
    - `SpectatorCountChanged` y la notificación de espectadores se publican desde
      `SpectatorshipLifecycleManager.publishFor(...)`, invocado por `SpectateMatchCommandHandler` y
      `StopSpectatingMatchCommandHandler`, que hacen `spectatorshipRepository.save(...)` → corren
      dentro
      de una transacción de escritura. Por lo tanto están expuestos al rollback/retry (categoría B).
    - `ProfileEventNotification` se publica desde `ProfileNotificationEventTranslator`, dentro de
      los
      flujos de tracking de logros/stats que persisten cambios de perfil → in-tx.
- **Alternativas consideradas**: dejarlos como están si corrieran fuera de tx; descartado tras
  verificar que sí corren en tx de escritura.

## Decisión 5 — Salvaguarda contra reintroducción (FR-009)

- **Decisión**: Introducir el marcador `InTransactionApplicationEvent` y una regla ArchUnit en
  `CleanArchitectureTest`: toda clase concreta que implemente `ApplicationEvent` debe implementar
  exactamente uno de `{PostCommitApplicationEvent, InTransactionApplicationEvent}`.
- **Rationale**: Obliga a una decisión consciente por cada evento nuevo; el build falla si se omite.
  Es explícito (no depende de nombres) y vive donde ya se enforced la arquitectura.
- **Alternativas consideradas**:
    - Allow-list en un test (todos post-commit salvo lista cerrada): más simple pero menos
      autodocumentado en la clase del evento; descartado a favor del marcador simétrico.
    - Solo documentación en CLAUDE.md: no es enforceable; insuficiente para "que no nos vuelva a
      pasar".

## Confirmaciones de comportamiento (sin incógnitas restantes)

- El dispatch de domain→application events es **síncrono** (`CompositeEventDispatcher`,
  `InProcessApplicationEventPublisher`).
- El pipeline `retryTransactionalPipeline` es `[optimisticLockRetryBehavior, transactionalBehavior]`
  → el retry corre **por fuera** de la tx; cada intento es una tx nueva. Esto confirma el origen de
  los duplicados (categoría B) y que post-commit los elimina.
- El fallo de envío post-commit ya se observa vía `EventNotifierHealthRegistry`.
