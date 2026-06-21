# Notificaciones y timing de eventos de aplicación

> Decisiones de dominio no obvias. Verificá contra el estado actual del código antes de asumir que
> siguen vigentes.

## Notificaciones WebSocket: siempre post-commit

Toda notificación en tiempo real al usuario (push WebSocket / application event de notificación)
DEBE emitirse **después** del commit, nunca dentro de la transacción. Se logra implementando
`PostCommitApplicationEvent` en el evento (el `TransactionalApplicationEventPublisher` lo difiere a
`afterCommit`).

**Dos categorías de race condition que esto evita:**

- **Categoría A (404 / datos viejos):** el cliente recibe el push con un id, hace `GET` del recurso
  y recibe 404 porque la tx aún no commiteó. Casos reproducidos: partida rápida, rematch confirmado,
  inicio de partida, creación/inicio de liga/copa, invitación aceptada y avance de competición.
- **Categoría B (fantasma / duplicado):** el push sale antes del commit; si la tx hace rollback el
  cliente vio algo que no se persistió, y como el `retryTransactionalPipeline` reintenta **por
  fuera** de la tx (`[optimisticLockRetryBehavior, transactionalBehavior]`), un reintento reenvía el
  push. Caso claro: chat.

**Excepción — eventos de coordinación (categoría C):** los eventos que disparan **escrituras**
atómicas en otro agregado (avance de liga/copa, logros de perfil, creación de sesión de rematch en
`CompetitionDomainEventTranslator`) NO se mueven a post-commit — partirían la atomicidad. Se separa:
la escritura queda in-tx y solo la **notificación derivada** va post-commit.

**Cómo aplicar:** al agregar un nuevo application event de notificación, marcarlo
`PostCommitApplicationEvent` por defecto. La regla ArchUnit en `CleanArchitectureTest` obliga a todo
application event a declarar su timing implementando exactamente uno de los dos marcadores
(`PostCommitApplicationEvent` o `InTransactionApplicationEvent`). Trabajo formalizado en
`specs/010-post-commit-events/spec.md`.

## Registro de partidas: decorator post-commit por fuera del pipeline

El registro de cada decisión jugable (`match_action_log`, feature 015) se captura con
`GameplayRecordingDecorator`, que envuelve a los 6 use cases de acción **por fuera** del
`retryTransactionalPipeline` en `MatchUseCaseConfiguration`
(`recordingDecorator.decorate(retryTransactionalPipeline.wrap(handler))`). Así su lógica corre
**después del commit** de la jugada y el adapter (`JpaGameplayRecorderAdapter`) escribe en su
**propia transacción**; un fallo de registro se traga y loguea (FR-010), nunca revierte ni
interrumpe la jugada.

Como humano y bot ejecutan a través de esos mismos beans, ambos quedan registrados sin
instrumentación específica del bot. El estado se reusa del read-model `MatchSnapshot` (no es un
agregado nuevo). No mover esa captura dentro del pipeline ni al `ExecuteBotTurnUseCase`
(orquestador).
