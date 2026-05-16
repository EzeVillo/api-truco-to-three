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
      case HAND_RESOLVED -> this.resolveHandAchievements(tracker, decisions);
      case FOLDED -> this.resolveFoldAchievements(tracker, decisions);
      case SCORE_CHANGED -> this.resolveScoreAchievements(tracker, decisions);
      default -> {
      }
    }
    return decisions;
  }

  private void resolveHandAchievements(final MatchAchievementTracker tracker,
      final List<AchievementUnlockDecision> decisions) {

    if (tracker.getLastHandWinnerSeat() == null || (tracker.getLastHandCardPlayerOne() == null
        && tracker.getLastHandCardPlayerTwo() == null)) {
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
          AchievementCode.WIN_HAND_UNCONTESTED_WITH_ANCHO_DE_ESPADA));
    }
  }

  private void resolveFoldAchievements(final MatchAchievementTracker tracker,
      final List<AchievementUnlockDecision> decisions) {

    if (tracker.getLastFoldedSeat() == null) {
      return;
    }

    if (tracker.getCardsPlayedInRound() == 0) {
      decisions.add(
          new AchievementUnlockDecision(tracker.resolvePlayer(tracker.getLastFoldedSeat()),
              AchievementCode.FOLD_BEFORE_ANY_CARD_IS_PLAYED));
    }
  }

  private void resolveScoreAchievements(final MatchAchievementTracker tracker,
      final List<AchievementUnlockDecision> decisions) {

    final var winnerSeat = this.resolveScoreWinnerSeat(tracker);
    if (winnerSeat != null) {
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
            AchievementCode.WIN_MATCH_THREE_ZERO_VIA_ACCEPTED_RETRUCO));
      }

      if (winnerPreviousScore == 2 && loserPreviousScore == 2 && winnerScore == 3
          && !tracker.isRoundHadCalls()) {
        decisions.add(new AchievementUnlockDecision(winner,
            AchievementCode.WIN_MATCH_FROM_2_2_WITHOUT_CALLS_IN_ROUND));
      }

      final var calls = tracker.getEnvidoCallsInRound();
      if (tracker.getLastEnvidoResponse() == EnvidoResponse.QUIERO
          && tracker.getLastEnvidoWinnerSeat() != null
          && winnerSeat == tracker.getLastEnvidoWinnerSeat()) {

        if (winnerPreviousScore == 2 && loserPreviousScore == 2
            && winnerSeat == tracker.getManoSeat() && tracker.getLastEnvidoPointsMano() != null
            && tracker.getLastEnvidoPointsMano() == 33 && tracker.getLastEnvidoPointsPie() != null
            && tracker.getLastEnvidoPointsPie() == 33 && calls.contains(EnvidoCall.FALTA_ENVIDO)) {
          decisions.add(new AchievementUnlockDecision(winner,
              AchievementCode.WIN_MATCH_AS_MANO_VIA_FALTA_ENVIDO_WITH_33_33_AT_2_2));
        }

        if (winnerPreviousScore == 0 && loserPreviousScore == 0 && calls.size() == 1 && (
            calls.contains(EnvidoCall.REAL_ENVIDO) || calls.contains(EnvidoCall.FALTA_ENVIDO))) {
          decisions.add(new AchievementUnlockDecision(winner,
              AchievementCode.WIN_MATCH_THREE_ZERO_VIA_REAL_OR_FALTA_ENVIDO));
        }
      }

      this.resolveBustAchievements(tracker, decisions);
    }
  }

  private void resolveBustAchievements(final MatchAchievementTracker tracker,
      final List<AchievementUnlockDecision> decisions) {

    final var bustedSeat = this.resolveScoreBustedSeat(tracker);
    if (bustedSeat == null) {
      return;
    }

    final var calls = tracker.getEnvidoCallsInRound();
    if (bustedSeat == tracker.getManoSeat()
        && tracker.getLastEnvidoResponse() == EnvidoResponse.QUIERO
        && bustedSeat == tracker.getLastEnvidoWinnerSeat()
        && tracker.previousScoreFor(bustedSeat) == 2
        && tracker.previousScoreFor(tracker.opposite(bustedSeat)) == 2
        && tracker.getLastEnvidoPointsMano() != null && tracker.getLastEnvidoPointsMano() == 0
        && tracker.getLastEnvidoPointsPie() != null && tracker.getLastEnvidoPointsPie() == 0
        && !calls.contains(EnvidoCall.FALTA_ENVIDO)) {
      decisions.add(
          new AchievementUnlockDecision(tracker.resolvePlayer(tracker.opposite(bustedSeat)),
              AchievementCode.WIN_MATCH_AS_PIE_MANO_BUSTS_ON_ENVIDO_WITH_0_0_AT_2_2));
    }

    if (tracker.getLastTrucoResponderSeat() == null) {
      return;
    }

    if (bustedSeat == tracker.getLastTrucoResponderSeat()
        && tracker.getLastTrucoCallerSeat() != null && tracker.isTrucoCalledWhenRivalHadNoCards()
        && tracker.getLastTrucoResponse() == TrucoResponse.QUIERO
        && tracker.getLastFoldedSeat() == tracker.getLastTrucoCallerSeat()) {
      decisions.add(
          new AchievementUnlockDecision(tracker.resolvePlayer(tracker.getLastTrucoCallerSeat()),
              AchievementCode.WIN_MATCH_BUST_RIVAL_VIA_FOLD_AFTER_ACCEPTED_TRUCO_WITH_NO_CARDS));
    }

    final var lastResponse = tracker.getLastTrucoResponse();
    final var lastCall = tracker.getLastTrucoResponseCall();

    if (lastResponse == TrucoResponse.QUIERO && lastCall == TrucoCall.VALE_CUATRO
        && tracker.getPreviousScorePlayerOne() == 0 && tracker.getPreviousScorePlayerTwo() == 0) {
      decisions.add(
          new AchievementUnlockDecision(tracker.resolvePlayer(tracker.opposite(bustedSeat)),
              AchievementCode.WIN_MATCH_BUST_OPPONENT_VIA_VALE_CUATRO_LOSS_AT_0_0));
      return;
    }

    if (tracker.getLastTrucoResponderSeat() == bustedSeat) {
      return;
    }

    final var responder = tracker.resolvePlayer(tracker.getLastTrucoResponderSeat());

    if (lastResponse == TrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO) {
      decisions.add(new AchievementUnlockDecision(responder,
          AchievementCode.WIN_MATCH_BUST_OPPONENT_VIA_QUIERO_Y_ME_VOY_AL_MAZO));
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

  private PlayerSeat resolveScoreBustedSeat(final MatchAchievementTracker tracker) {

    if (tracker.getScorePlayerOne() > 3 && tracker.getPreviousScorePlayerOne() <= 3) {
      return PlayerSeat.PLAYER_ONE;
    }
    if (tracker.getScorePlayerTwo() > 3 && tracker.getPreviousScorePlayerTwo() <= 3) {
      return PlayerSeat.PLAYER_TWO;
    }
    return null;
  }

}
