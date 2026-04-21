package com.villo.truco.profile.domain.model;

import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.EnvidoResponse;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.model.match.valueobjects.TrucoResponse;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import java.util.ArrayList;
import java.util.List;

public final class AchievementPolicy {

  public List<AchievementUnlockDecision> decideUnlocks(final MatchAchievementTracker tracker) {

    final var decisions = new ArrayList<AchievementUnlockDecision>();
    switch (tracker.getLastUpdateType()) {
      case ENVIDO_RESOLVED -> this.resolveEnvidoAchievements(tracker, decisions);
      case TRUCO_RESPONDED -> this.resolveTrucoResponseAchievements(tracker, decisions);
      case HAND_RESOLVED -> this.resolveHandAchievements(tracker, decisions);
      case FOLDED -> this.resolveFoldAchievements(tracker, decisions);
      case SCORE_CHANGED -> this.resolveScoreAchievements(tracker, decisions);
      default -> {
      }
    }
    return decisions;
  }

  private void resolveEnvidoAchievements(final MatchAchievementTracker tracker,
      final List<AchievementUnlockDecision> decisions) {

    if (tracker.getLastEnvidoResponse() != EnvidoResponse.QUIERO
        || tracker.getLastEnvidoWinnerSeat() == null) {
      return;
    }

    final var winnerSeat = tracker.getLastEnvidoWinnerSeat();
    final var winner = tracker.resolvePlayer(winnerSeat);
    final var calls = tracker.getEnvidoCallsInRound();

    if (tracker.getScorePlayerOne() == 2 && tracker.getScorePlayerTwo() == 2
        && winnerSeat == tracker.getManoSeat() && tracker.getLastEnvidoPointsMano() != null
        && tracker.getLastEnvidoPointsMano() == 0
        && calls.contains(EnvidoCall.ENVIDO) && !calls.contains(EnvidoCall.REAL_ENVIDO)
        && !calls.contains(EnvidoCall.FALTA_ENVIDO)) {
      decisions.add(
          new AchievementUnlockDecision(winner, AchievementCode.WIN_ENVIDO_2_2_MANO_ZERO));
    }

    if (tracker.getScorePlayerOne() == 2 && tracker.getScorePlayerTwo() == 2
        && winnerSeat == tracker.getManoSeat() && tracker.getLastEnvidoPointsMano() != null
        && tracker.getLastEnvidoPointsMano() == 33
        && calls.contains(EnvidoCall.FALTA_ENVIDO)) {
      decisions.add(new AchievementUnlockDecision(winner,
          AchievementCode.WIN_FALTA_ENVIDO_2_2_MANO_33));
    }

    if (tracker.getScorePlayerOne() == 0 && tracker.getScorePlayerTwo() == 0
        && (calls.contains(EnvidoCall.REAL_ENVIDO) || calls.contains(EnvidoCall.FALTA_ENVIDO))) {
      decisions.add(
          new AchievementUnlockDecision(winner, AchievementCode.WIN_REAL_OR_FALTA_FROM_0_0));
    }
  }

  private void resolveTrucoResponseAchievements(final MatchAchievementTracker tracker,
      final List<AchievementUnlockDecision> decisions) {

    if (tracker.getLastTrucoResponderSeat() == null || tracker.getLastTrucoResponse() == null) {
      return;
    }

    final var responderSeat = tracker.getLastTrucoResponderSeat();
    final var winnerSeat = tracker.opposite(responderSeat);
    final var winner = tracker.resolvePlayer(winnerSeat);

    if (tracker.getLastTrucoResponse() == TrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO) {
      decisions.add(new AchievementUnlockDecision(winner,
          AchievementCode.WIN_BY_QUIERO_Y_ME_VOY_AL_MAZO));
    }

    if (tracker.getLastTrucoResponse() == TrucoResponse.NO_QUIERO
        && tracker.scoreFor(responderSeat) == 2) {
      decisions.add(new AchievementUnlockDecision(winner,
          AchievementCode.WIN_TRUCO_AGAINST_BUSTED_OPPONENT_WITHOUT_ACCEPT_AND_FOLD));
    }
  }

  private void resolveHandAchievements(final MatchAchievementTracker tracker,
      final List<AchievementUnlockDecision> decisions) {

    if (tracker.getLastHandWinnerSeat() == null || tracker.getLastHandCardPlayerOne() == null
        && tracker.getLastHandCardPlayerTwo() == null) {
      return;
    }

    final var winnerSeat = tracker.getLastHandWinnerSeat();
    final var winnerCard = winnerSeat == PlayerSeat.PLAYER_ONE ? tracker.getLastHandCardPlayerOne()
        : tracker.getLastHandCardPlayerTwo();
    final var loserCard = winnerSeat == PlayerSeat.PLAYER_ONE ? tracker.getLastHandCardPlayerTwo()
        : tracker.getLastHandCardPlayerOne();

    if (winnerCard != null && loserCard == null && winnerCard.number() == 1
        && winnerCard.suit() == Suit.ESPADA) {
      decisions.add(new AchievementUnlockDecision(tracker.resolvePlayer(winnerSeat),
          AchievementCode.WIN_BY_CUTTING_ROUND_WITH_ANCHO_DE_ESPADA));
    }
  }

  private void resolveFoldAchievements(final MatchAchievementTracker tracker,
      final List<AchievementUnlockDecision> decisions) {

    if (tracker.getLastFoldedSeat() == null) {
      return;
    }

    if (tracker.getPlayedHandsInRound() == 0) {
      decisions.add(new AchievementUnlockDecision(tracker.resolvePlayer(tracker.getLastFoldedSeat()),
          AchievementCode.FOLD_ON_FIRST_HAND));
    }
  }

  private void resolveScoreAchievements(final MatchAchievementTracker tracker,
      final List<AchievementUnlockDecision> decisions) {

    final var winnerSeat = this.resolveScoreWinnerSeat(tracker);
    if (winnerSeat == null) {
      return;
    }

    final var winner = tracker.resolvePlayer(winnerSeat);
    final var loserSeat = tracker.opposite(winnerSeat);

    final var winnerPreviousScore = tracker.previousScoreFor(winnerSeat);
    final var loserPreviousScore = tracker.previousScoreFor(loserSeat);
    final var winnerScore = tracker.scoreFor(winnerSeat);
    final var loserScore = tracker.scoreFor(loserSeat);

    if (winnerPreviousScore == 0 && loserPreviousScore == 0 && winnerScore == 3 && loserScore == 0
        && tracker.getLastTrucoResponse() == TrucoResponse.QUIERO
        && tracker.getLastTrucoResponseCall() == TrucoCall.RETRUCO) {
      decisions.add(new AchievementUnlockDecision(winner,
          AchievementCode.WIN_RETRUCO_FROM_0_0_TO_3));
    }

    if (winnerPreviousScore == 2 && loserPreviousScore == 2 && winnerScore == 3
        && !tracker.isRoundHadCalls()) {
      decisions.add(new AchievementUnlockDecision(winner,
          AchievementCode.WIN_AT_2_2_WITHOUT_CALLS_IN_ROUND));
    }

    if (winnerScore == 3 && tracker.hasLostAcceptedValeCuatroByBust(winnerSeat)) {
      decisions.add(new AchievementUnlockDecision(winner,
          AchievementCode.WIN_AFTER_LOSING_ACCEPTED_VALE_CUATRO_BY_BUST));
    }
  }

  private PlayerSeat resolveScoreWinnerSeat(final MatchAchievementTracker tracker) {

    final var changedPlayerOne = tracker.getScorePlayerOne() != tracker.getPreviousScorePlayerOne();
    final var changedPlayerTwo = tracker.getScorePlayerTwo() != tracker.getPreviousScorePlayerTwo();
    if (changedPlayerOne == changedPlayerTwo) {
      return null;
    }
    return changedPlayerOne ? PlayerSeat.PLAYER_ONE : PlayerSeat.PLAYER_TWO;
  }
}
