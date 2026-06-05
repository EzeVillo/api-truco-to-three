package com.villo.truco.application.events;

import com.villo.truco.application.dto.UserPresenceDTO;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

/**
 * Notificacion de un cambio de presencia/ocupacion de un usuario. Lleva un unico destinatario (el
 * dueño del estado) y su snapshot de presencia, para entregarse a {@code /user/queue/presence}.
 *
 * <p>Es {@link PostCommitApplicationEvent}: el snapshot se resuelve dentro de la transaccion (al
 * despachar el domain event, tras persistir el agregado), pero la entrega por WebSocket se difiere
 * hasta despues del commit, evitando empujar estados que un rollback dejaria sin efecto.
 */
public record PresenceEventNotification(PlayerId recipient, String eventType, long timestamp,
                                        UserPresenceDTO snapshot) implements
    PostCommitApplicationEvent {

  public static final String EVENT_TYPE = "PRESENCE_UPDATED";

}
