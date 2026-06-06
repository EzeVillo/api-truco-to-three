# Contratos — Notificaciones post-commit

## Sin cambios de contrato

Esta feature **no modifica ningún contrato externo**:

- **REST**: no agrega, quita ni cambia el shape de ningún endpoint.
- **WebSocket/STOMP**: no agrega ni quita `eventType`; los destinos (`/user/queue/match`,
  `/user/queue/league`, `/user/queue/cup`, `/user/queue/chat`, `/user/queue/social`, etc.) y los
  payloads se mantienen idénticos.
- **Enums / reglas de negocio expuestas**: sin cambios.

## Único cambio observable: el *timing* de entrega

La diferencia es **cuándo** se entrega cada notificación: ahora se emite **después** del commit de
la
transacción que la originó, en lugar de antes. Esto es una corrección de garantía de consistencia,
no
un cambio de contrato:

- **Garantía nueva (implícita)**: cuando un cliente recibe una notificación que referencia un
  recurso
  (por id), una consulta REST inmediata de ese recurso lo encontrará (no habrá 404 por carrera con
  el
  commit).
- **Garantía nueva (implícita)**: si una operación se revierte, su notificación no se emite; si se
  reintenta, se emite una sola vez.

## Impacto en `docs/CONTRATOS_API.md`

Revisar si el documento afirma algo sobre el orden de entrega de notificaciones respecto a la
persistencia. Si afirma o sugiere entrega "inmediata"/"durante" la operación, actualizar a "tras la
confirmación del cambio". Si no menciona el timing, **no requiere cambios**.
