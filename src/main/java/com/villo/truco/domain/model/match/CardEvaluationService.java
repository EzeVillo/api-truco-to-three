package com.villo.truco.domain.model.match;

import com.villo.truco.domain.shared.cards.valueobjects.Card;
import java.util.List;

public final class CardEvaluationService {

  private CardEvaluationService() {

  }

  public static int trucoValue(final Card card) {

    return TrucoCardValue.of(card);
  }

  public static int envidoScore(final List<Card> cards) {

    return EnvidoCalculator.calculate(cards);
  }

}
