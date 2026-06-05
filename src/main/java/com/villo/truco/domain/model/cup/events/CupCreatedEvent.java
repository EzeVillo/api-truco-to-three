package com.villo.truco.domain.model.cup.events;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

/**
 * Emitido cuando un jugador crea una copa y queda como host a la espera de participantes. Marca que
 * el creador pasa a estar ocupado (no puede entrar a otro recurso) ya en estado de espera.
 */
public final class CupCreatedEvent extends CupDomainEvent {

  public CupCreatedEvent(final CupId cupId, final List<PlayerId> participants) {

    super("CUP_CREATED", cupId, participants);
  }

}
