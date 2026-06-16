package com.villo.truco.application.usecases.commands;

import com.villo.truco.domain.model.bot.EnvidoScoring;
import com.villo.truco.domain.model.match.CardEvaluationService;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import java.util.List;

public final class MatchEnvidoScoring implements EnvidoScoring {

  @Override
  public int of(final List<Card> cards) {

    return CardEvaluationService.envidoScore(cards);
  }

}
