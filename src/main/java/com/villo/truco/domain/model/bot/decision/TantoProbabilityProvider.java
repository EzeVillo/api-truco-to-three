package com.villo.truco.domain.model.bot.decision;

import com.villo.truco.domain.model.bot.EnvidoProbabilityCalculator;
import com.villo.truco.domain.model.bot.EnvidoScoring;
import com.villo.truco.domain.model.bot.valueobjects.BotCard;
import java.util.List;

public final class TantoProbabilityProvider {

  private final double winProbability;

  public TantoProbabilityProvider(final EnvidoScoring scoring, final List<BotCard> myCards,
      final int envidoScore, final boolean isMano, final BotCard rivalCardPlayed) {

    this.winProbability = EnvidoProbabilityCalculator.probabilityBotWinsTanto(scoring, myCards,
        envidoScore, isMano, rivalCardPlayed);
  }

  private TantoProbabilityProvider(final double winProbability) {

    this.winProbability = winProbability;
  }

  /** Factory para tests que necesitan inyectar una probabilidad conocida directamente. */
  public static TantoProbabilityProvider withKnownProbability(final double probability) {

    return new TantoProbabilityProvider(probability);
  }

  public double probabilityBotWinsTanto() {

    return winProbability;
  }

  public boolean moreLikelyToWinTanto() {

    return winProbability > 0.5;
  }

  public boolean moreLikelyToLoseTanto() {

    return winProbability < 0.5;
  }

  public boolean tie() {

    return winProbability == 0.5;
  }

}
