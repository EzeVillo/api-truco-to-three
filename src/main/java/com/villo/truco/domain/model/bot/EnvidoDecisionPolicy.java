package com.villo.truco.domain.model.bot;

import com.villo.truco.domain.model.bot.exceptions.PendingEnvidoCallRequiredException;
import com.villo.truco.domain.model.bot.valueobjects.BotCard;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoCall;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoLevel;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoResponse;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView;
import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

final class EnvidoDecisionPolicy {

  private static final int GOOD_ENVIDO_THRESHOLD = 26;
  private static final int FALTA_ENVIDO_THRESHOLD = 29;
  private static final int SUICIDAL_TRAP_MAX_ENVIDO = 19;
  private static final double CALL_BASE = 0.15;
  private static final double RESPOND_ACCEPT_BASE = 0.40;
  private static final double BOTH_AT_MATCH_POINT_THRESHOLD = 0.5;

  private final BotPersonality personality;
  private final Random random;

  EnvidoDecisionPolicy(final BotPersonality personality, final Random random) {

    this.personality = personality;
    this.random = random;
  }

  Optional<BotEnvidoCall> decideCall(final List<BotEnvidoCall> availableCalls,
      final int envidoScore, final int myScore, final int rivalScore, final int pointsToWin,
      final boolean isMano, final boolean isFirstCall, final List<BotCard> myCards,
      final BotCard rivalCardPlayed) {

    if (availableCalls.isEmpty()) {
      return Optional.empty();
    }

    if (isFirstCall && myScore == pointsToWin - 1 && rivalScore == pointsToWin - 1) {
      final var forced = decideBothAtMatchPoint(availableCalls, myCards, envidoScore, isMano,
          rivalCardPlayed);
      if (forced.isPresent()) {
        return forced;
      }
    }

    final var safeCalls = availableCalls.stream().filter(
        call -> this.callRiskProfile(call, envidoScore, myScore, rivalScore, pointsToWin)
            == CallRiskProfile.SAFE).toList();
    final var viableCalls = safeCalls.isEmpty() ? availableCalls.stream().filter(
        call -> this.callRiskProfile(call, envidoScore, myScore, rivalScore, pointsToWin)
            == CallRiskProfile.SUICIDAL_TRAP).toList() : safeCalls;

    if (viableCalls.isEmpty()) {
      return Optional.empty();
    }

    if (myScore == pointsToWin - 1) {
      return decideAtMatchPoint(viableCalls, envidoScore, rivalScore, pointsToWin, isFirstCall);
    }

    if (isFirstCall && isMano && envidoScore >= GOOD_ENVIDO_THRESHOLD) {
      final var pescadorChance = this.personality.pescador() / 100.0;
      if (this.random.nextDouble() < pescadorChance) {
        return Optional.empty();
      }
    }

    final var scoreFactor = envidoScore / 33.0;
    final var envidosoFactor = this.personality.envidoso() / 100.0;
    final var mentirosoBoost = this.personality.mentiroso() / 300.0;
    final var callProbability = CALL_BASE + envidosoFactor * scoreFactor + mentirosoBoost;

    if (this.random.nextDouble() > callProbability) {
      return Optional.empty();
    }

    if (safeCalls.isEmpty()) {
      return Optional.of(this.pickLowestLevel(viableCalls));
    }
    return this.pickJustifiedLevel(viableCalls, envidoScore);
  }

  BotEnvidoResponse decideResponse(final int envidoScore, final int myScore, final int rivalScore,
      final int pointsToWin, final BotMatchView.PendingEnvidoOutcome pendingOutcome) {

    if (pendingOutcome == null) {
      throw new PendingEnvidoCallRequiredException();
    }

    final var ptsIfBotWins = pendingOutcome.acceptedPointsIfBotWins();
    final var ptsIfRivalWins = pendingOutcome.acceptedPointsIfRivalWins();
    final var rejectedPts = pendingOutcome.rejectedPoints();

    final var botDiesIfWins = myScore + ptsIfBotWins > pointsToWin;
    final var rivalDiesIfWins = rivalScore + ptsIfRivalWins > pointsToWin;
    final var rivalDiesIfRejects = rivalScore + rejectedPts > pointsToWin;
    final var rivalWinsIfRejects = rivalScore + rejectedPts == pointsToWin;

    if (rivalDiesIfRejects) {
      return BotEnvidoResponse.NO_QUIERO;
    }

    if (rivalDiesIfWins && !botDiesIfWins) {
      return BotEnvidoResponse.QUIERO;
    }

    if (rivalDiesIfWins) {
      if (rivalWinsIfRejects) {
        return BotEnvidoResponse.QUIERO;
      }
      if (envidoScore < GOOD_ENVIDO_THRESHOLD) {
        return BotEnvidoResponse.QUIERO;
      }
      return BotEnvidoResponse.NO_QUIERO;
    }

    if (botDiesIfWins) {
      return BotEnvidoResponse.NO_QUIERO;
    }

    if (rivalWinsIfRejects) {
      return BotEnvidoResponse.QUIERO;
    }

    final var rivalWinsGameIfBotLoses = rivalScore + ptsIfRivalWins >= pointsToWin;
    if (rivalWinsGameIfBotLoses) {
      return BotEnvidoResponse.NO_QUIERO;
    }

    return this.probabilisticResponse(envidoScore);
  }

  private BotEnvidoResponse probabilisticResponse(final int envidoScore) {

    final var scoreFactor = envidoScore / 33.0;
    final var envidosoBoost = this.personality.envidoso() / 100.0;
    final var acceptProbability = scoreFactor * (RESPOND_ACCEPT_BASE + envidosoBoost);

    if (this.random.nextDouble() < acceptProbability) {
      return BotEnvidoResponse.QUIERO;
    }
    return BotEnvidoResponse.NO_QUIERO;
  }

  private CallRiskProfile callRiskProfile(final BotEnvidoCall call, final int envidoScore,
      final int myScore, final int rivalScore, final int pointsToWin) {

    final var botDiesIfRejected =
        myScore + call.rejectedPointsIfRivalDeclines() > pointsToWin;
    if (botDiesIfRejected) {
      return CallRiskProfile.FORBIDDEN;
    }

    final var botDiesIfWins = myScore + call.acceptedPointsIfBotWins() > pointsToWin;
    if (!botDiesIfWins) {
      return CallRiskProfile.SAFE;
    }

    final var rivalDiesIfWins = rivalScore + call.acceptedPointsIfRivalWins() > pointsToWin;
    final var rivalDiesIfRejects = rivalScore + call.rejectedPointsIfRivalDeclines() > pointsToWin;

    if (rivalDiesIfRejects) {
      return CallRiskProfile.SAFE;
    }

    if (rivalDiesIfWins && envidoScore <= SUICIDAL_TRAP_MAX_ENVIDO) {
      return CallRiskProfile.SUICIDAL_TRAP;
    }

    return CallRiskProfile.FORBIDDEN;
  }

  private Optional<BotEnvidoCall> decideAtMatchPoint(final List<BotEnvidoCall> availableCalls,
      final int envidoScore, final int rivalScore, final int pointsToWin,
      final boolean isFirstCall) {

    if (!isFirstCall || rivalScore < pointsToWin - 1) {
      return Optional.empty();
    }

    if (envidoScore >= GOOD_ENVIDO_THRESHOLD) {
      final var falta = availableCalls.stream()
          .filter(o -> o.level() == BotEnvidoLevel.FALTA_ENVIDO).findFirst();
      if (falta.isPresent()) {
        return falta;
      }
    } else {
      return availableCalls.stream().filter(o -> o.level() == BotEnvidoLevel.ENVIDO).findFirst();
    }
    return Optional.empty();
  }

  /**
   * Empate en punto de partida (típicamente 2-2 en una partida a 3). Por la regla de punto exacto,
   * el envido empuja a 4 y revienta a quien lo gana, mientras que la falta hace llegar a 3 al
   * ganador. Por eso el bot SIEMPRE canta, eligiendo según la probabilidad real de ganar el tanto
   * (calculada en vivo con sus cartas): si es favorito (≥ 0,5) canta falta para ganar; si no, canta
   * envido como trampa para que el rival gane el tanto y se pase. La personalidad no interviene.
   *
   * <p>Como mano entra siempre. Como pie solo entra si el rival ya jugó una carta que el bot no
   * puede matar (si la puede matar, conserva chances de truco y cae en la lógica habitual).
   */
  private Optional<BotEnvidoCall> decideBothAtMatchPoint(final List<BotEnvidoCall> availableCalls,
      final List<BotCard> myCards, final int envidoScore, final boolean isMano,
      final BotCard rivalCardPlayed) {

    if (!isMano) {
      if (rivalCardPlayed == null) {
        return Optional.empty();
      }
      final var canBeatRivalCard = myCards.stream()
          .anyMatch(card -> card.trucoRank() > rivalCardPlayed.trucoRank());
      if (canBeatRivalCard) {
        return Optional.empty();
      }
    }

    final var winProbability = EnvidoProbabilityCalculator.probabilityBotWinsTanto(myCards,
        envidoScore, isMano, rivalCardPlayed);
    final var targetLevel = winProbability >= BOTH_AT_MATCH_POINT_THRESHOLD
        ? BotEnvidoLevel.FALTA_ENVIDO : BotEnvidoLevel.ENVIDO;

    return availableCalls.stream().filter(call -> call.level() == targetLevel).findFirst();
  }

  private Optional<BotEnvidoCall> pickJustifiedLevel(final List<BotEnvidoCall> availableCalls,
      final int envidoScore) {

    final BotEnvidoLevel maxJustifiedLevel;
    if (envidoScore >= FALTA_ENVIDO_THRESHOLD) {
      maxJustifiedLevel = BotEnvidoLevel.FALTA_ENVIDO;
    } else if (envidoScore >= GOOD_ENVIDO_THRESHOLD) {
      maxJustifiedLevel = BotEnvidoLevel.REAL_ENVIDO;
    } else {
      maxJustifiedLevel = BotEnvidoLevel.ENVIDO;
    }

    return availableCalls.stream()
        .filter(o -> o.level().ordinal() <= maxJustifiedLevel.ordinal())
        .max(Comparator.comparingInt(o -> o.level().ordinal()));
  }

  private BotEnvidoCall pickLowestLevel(final List<BotEnvidoCall> availableCalls) {

    return availableCalls.stream().filter(o -> o.level() == BotEnvidoLevel.ENVIDO).findFirst()
        .or(() -> availableCalls.stream().filter(o -> o.level() == BotEnvidoLevel.REAL_ENVIDO)
            .findFirst()).orElse(availableCalls.getFirst());
  }

  private enum CallRiskProfile {
    SAFE, SUICIDAL_TRAP, FORBIDDEN
  }

}
