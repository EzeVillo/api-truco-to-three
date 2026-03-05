package com.villo.truco.domain.model.match.exceptions;

import com.villo.truco.domain.model.match.valueobjects.Card;
import com.villo.truco.domain.shared.DomainException;

public final class CardNotInHandException extends DomainException {

  public CardNotInHandException(final Card card) {

    super("Card not in hand: " + card.toString());
  }

}
