package com.villo.truco.application.events;

/**
 * Marca un evento de coordinación que DEBE procesarse dentro de la transaccion que lo origino,
 * porque dispara escrituras atomicas en otro agregado.
 *
 * <p>Contrapartida de {@link PostCommitApplicationEvent}: las notificaciones al usuario van
 * post-commit; los eventos de coordinacion con escrituras atomicas van in-transaction. Toda clase
 * concreta que implemente {@link ApplicationEvent} debe declarar exactamente uno de los dos
 * marcadores (verificado por ArchUnit).
 */
public interface InTransactionApplicationEvent extends ApplicationEvent {

}
