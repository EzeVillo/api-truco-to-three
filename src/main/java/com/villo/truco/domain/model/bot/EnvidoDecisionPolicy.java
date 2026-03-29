package com.villo.truco.domain.model.bot;

import com.villo.truco.domain.model.bot.exceptions.PendingEnvidoCallRequiredException;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoCall;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoLevel;
import com.villo.truco.domain.model.bot.valueobjects.BotEnvidoResponse;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView;
import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import java.util.List;
import java.util.Optional;
import java.util.Random;

final class EnvidoDecisionPolicy {

  private static final int GOOD_ENVIDO_THRESHOLD = 26;
  private static final int FALTA_ENVIDO_THRESHOLD = 29;
  private static final double CALL_BASE = 0.15;
  private static final double RESPOND_ACCEPT_BASE = 0.40;

  private final BotPersonality personality;
  private final Random random;

  EnvidoDecisionPolicy(final BotPersonality personality, final Random random) {

    this.personality = personality;
    this.random = random;
  }

  Optional<BotEnvidoCall> decideCall(final List<BotEnvidoCall> availableCalls,
      final int envidoScore, final int myScore, final int rivalScore, final int pointsToWin,
      final boolean isMano, final boolean isFirstCall) {

    if (availableCalls.isEmpty()) {
      return Optional.empty();
    }

    final var viableCalls = availableCalls.stream()
        .filter(call -> this.isSafeEnoughToCall(call, envidoScore, myScore, rivalScore, pointsToWin))
        .toList();

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

    return Optional.of(this.pickCallLevel(viableCalls, envidoScore));
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

    return probabilisticResponse(envidoScore);
  }

  private BotEnvidoResponse probabilisticResponse(final int envidoScore) {

    final var scoreFactor = envidoScore / 33.0;
    final var envidosoBoost = this.personality.envidoso() / 100.0;
    final var acceptProbability = RESPOND_ACCEPT_BASE + envidosoBoost * scoreFactor;

    if (this.random.nextDouble() < acceptProbability) {
      return BotEnvidoResponse.QUIERO;
    }
    return BotEnvidoResponse.NO_QUIERO;
  }

  private boolean isSafeEnoughToCall(final BotEnvidoCall call, final int envidoScore,
      final int myScore, final int rivalScore, final int pointsToWin) {

    final var botDiesIfWins = myScore + call.acceptedPointsIfBotWins() > pointsToWin;
    if (!botDiesIfWins) {
      return true;
    }

    final var rivalDiesIfWins = rivalScore + call.acceptedPointsIfRivalWins() > pointsToWin;
    final var rivalDiesIfRejects =
        rivalScore + call.rejectedPointsIfRivalDeclines() > pointsToWin;

    if (rivalDiesIfRejects) {
      return true;
    }

    if (rivalDiesIfWins) {
      return envidoScore < GOOD_ENVIDO_THRESHOLD;
    }

    return false;
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

  private BotEnvidoCall pickCallLevel(final List<BotEnvidoCall> availableCalls,
      final int envidoScore) {

    if (envidoScore >= FALTA_ENVIDO_THRESHOLD) {
      final var falta = availableCalls.stream()
          .filter(o -> o.level() == BotEnvidoLevel.FALTA_ENVIDO).findFirst();
      if (falta.isPresent()) {
        return falta.get();
      }
    }
    if (envidoScore >= GOOD_ENVIDO_THRESHOLD) {
      final var realEnvido = availableCalls.stream()
          .filter(o -> o.level() == BotEnvidoLevel.REAL_ENVIDO).findFirst();
      if (realEnvido.isPresent()) {
        return realEnvido.get();
      }
    }
    return availableCalls.stream().filter(o -> o.level() == BotEnvidoLevel.ENVIDO).findFirst()
        .orElse(availableCalls.getFirst());
  }

}
