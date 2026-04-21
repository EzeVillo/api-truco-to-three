package com.villo.truco.profile.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.EnvidoResponse;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.model.match.valueobjects.TrucoResponse;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AchievementPolicy")
class AchievementPolicyTest {

  private final AchievementPolicy policy = new AchievementPolicy();

  @Test
  @DisplayName("desbloquea WIN_ENVIDO_2_2_MANO_ZERO")
  void unlocksWinEnvido2_2ManoZero() {

    final var tracker = this.newTracker();
    tracker.onScoreChanged(2, 2);
    tracker.onRoundStarted(2, PlayerSeat.PLAYER_ONE);
    tracker.onEnvidoCalled(EnvidoCall.ENVIDO);
    tracker.onEnvidoResolved(EnvidoResponse.QUIERO, PlayerSeat.PLAYER_ONE, 0, null);

    assertThat(this.policy.decideUnlocks(tracker)).contains(
        new AchievementUnlockDecision(tracker.getPlayerOne(),
            AchievementCode.WIN_ENVIDO_2_2_MANO_ZERO));
  }

  @Test
  @DisplayName("desbloquea WIN_FALTA_ENVIDO_2_2_MANO_33")
  void unlocksWinFaltaEnvido2_2Mano33() {

    final var tracker = this.newTracker();
    tracker.onScoreChanged(2, 2);
    tracker.onRoundStarted(2, PlayerSeat.PLAYER_ONE);
    tracker.onEnvidoCalled(EnvidoCall.FALTA_ENVIDO);
    tracker.onEnvidoResolved(EnvidoResponse.QUIERO, PlayerSeat.PLAYER_ONE, 33, null);

    assertThat(this.policy.decideUnlocks(tracker)).contains(
        new AchievementUnlockDecision(tracker.getPlayerOne(),
            AchievementCode.WIN_FALTA_ENVIDO_2_2_MANO_33));
  }

  @Test
  @DisplayName("desbloquea WIN_BY_QUIERO_Y_ME_VOY_AL_MAZO")
  void unlocksWinByQuieroYMeVoyAlMazo() {

    final var tracker = this.newTracker();
    tracker.onTrucoResponded(PlayerSeat.PLAYER_TWO, TrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO,
        TrucoCall.TRUCO);

    assertThat(this.policy.decideUnlocks(tracker)).contains(
        new AchievementUnlockDecision(tracker.getPlayerOne(),
            AchievementCode.WIN_BY_QUIERO_Y_ME_VOY_AL_MAZO));
  }

  @Test
  @DisplayName("desbloquea WIN_BY_CUTTING_ROUND_WITH_ANCHO_DE_ESPADA")
  void unlocksWinByCuttingRoundWithAnchoDeEspada() {

    final var tracker = this.newTracker();
    tracker.onHandResolved(Card.of(Suit.ESPADA, 1), null, PlayerSeat.PLAYER_ONE);

    assertThat(this.policy.decideUnlocks(tracker)).contains(
        new AchievementUnlockDecision(tracker.getPlayerOne(),
            AchievementCode.WIN_BY_CUTTING_ROUND_WITH_ANCHO_DE_ESPADA));
  }

  @Test
  @DisplayName("desbloquea FOLD_ON_FIRST_HAND")
  void unlocksFoldOnFirstHand() {

    final var tracker = this.newTracker();
    tracker.onFolded(PlayerSeat.PLAYER_TWO);

    assertThat(this.policy.decideUnlocks(tracker)).contains(
        new AchievementUnlockDecision(tracker.getPlayerTwo(), AchievementCode.FOLD_ON_FIRST_HAND));
  }

  @Test
  @DisplayName("desbloquea WIN_RETRUCO_FROM_0_0_TO_3")
  void unlocksWinRetrucoFromZeroZeroToThree() {

    final var tracker = this.newTracker();
    tracker.onTrucoResponded(PlayerSeat.PLAYER_TWO, TrucoResponse.QUIERO, TrucoCall.RETRUCO);
    tracker.onScoreChanged(3, 0);

    assertThat(this.policy.decideUnlocks(tracker)).contains(
        new AchievementUnlockDecision(tracker.getPlayerOne(),
            AchievementCode.WIN_RETRUCO_FROM_0_0_TO_3));
  }

  @Test
  @DisplayName("desbloquea WIN_REAL_OR_FALTA_FROM_0_0")
  void unlocksWinRealOrFaltaFromZeroZero() {

    final var tracker = this.newTracker();
    tracker.onEnvidoCalled(EnvidoCall.REAL_ENVIDO);
    tracker.onEnvidoResolved(EnvidoResponse.QUIERO, PlayerSeat.PLAYER_TWO, 29, 31);

    assertThat(this.policy.decideUnlocks(tracker)).contains(
        new AchievementUnlockDecision(tracker.getPlayerTwo(),
            AchievementCode.WIN_REAL_OR_FALTA_FROM_0_0));
  }

  @Test
  @DisplayName("desbloquea WIN_TRUCO_AGAINST_BUSTED_OPPONENT_WITHOUT_ACCEPT_AND_FOLD")
  void unlocksWinTrucoAgainstBustedOpponentWithoutAcceptAndFold() {

    final var tracker = this.newTracker();
    tracker.onScoreChanged(0, 2);
    tracker.onTrucoResponded(PlayerSeat.PLAYER_TWO, TrucoResponse.NO_QUIERO, TrucoCall.TRUCO);

    assertThat(this.policy.decideUnlocks(tracker)).contains(
        new AchievementUnlockDecision(tracker.getPlayerOne(),
            AchievementCode.WIN_TRUCO_AGAINST_BUSTED_OPPONENT_WITHOUT_ACCEPT_AND_FOLD));
  }

  @Test
  @DisplayName("desbloquea WIN_AT_2_2_WITHOUT_CALLS_IN_ROUND")
  void unlocksWinAtTwoTwoWithoutCallsInRound() {

    final var tracker = this.newTracker();
    tracker.onScoreChanged(2, 2);
    tracker.onRoundStarted(2, PlayerSeat.PLAYER_TWO);
    tracker.onScoreChanged(3, 2);

    assertThat(this.policy.decideUnlocks(tracker)).contains(
        new AchievementUnlockDecision(tracker.getPlayerOne(),
            AchievementCode.WIN_AT_2_2_WITHOUT_CALLS_IN_ROUND));
  }

  @Test
  @DisplayName("desbloquea WIN_AFTER_LOSING_ACCEPTED_VALE_CUATRO_BY_BUST")
  void unlocksWinAfterLosingAcceptedValeCuatroByBust() {

    final var tracker = this.newTracker();
    tracker.onTrucoResponded(PlayerSeat.PLAYER_TWO, TrucoResponse.QUIERO, TrucoCall.VALE_CUATRO);
    tracker.onScoreChanged(4, 0);
    tracker.onGameStarted(2);
    tracker.onRoundStarted(1, PlayerSeat.PLAYER_TWO);
    tracker.onScoreChanged(3, 0);

    assertThat(this.policy.decideUnlocks(tracker)).contains(
        new AchievementUnlockDecision(tracker.getPlayerOne(),
            AchievementCode.WIN_AFTER_LOSING_ACCEPTED_VALE_CUATRO_BY_BUST));
  }

  @Test
  @DisplayName("NO_QUIERO no desbloquea logros que requieren QUIERO")
  void noQuieroDoesNotUnlockAchievementsThatRequireQuiero() {

    final var tracker = this.newTracker();
    tracker.onEnvidoCalled(EnvidoCall.REAL_ENVIDO);
    tracker.onEnvidoResolved(EnvidoResponse.NO_QUIERO, PlayerSeat.PLAYER_ONE, null, null);

    final var decisions = this.policy.decideUnlocks(tracker);

    assertThat(decisions).noneMatch(
        d -> d.achievementCode() == AchievementCode.WIN_REAL_OR_FALTA_FROM_0_0);
  }

  @Test
  @DisplayName("QUIERO_Y_ME_VOY_AL_MAZO no cuenta para perder VALE_CUATRO jugado")
  void quieroYMeVoyAlMazoDoesNotCountForAcceptedValeCuatroBustLoss() {

    final var tracker = this.newTracker();
    tracker.onTrucoResponded(PlayerSeat.PLAYER_TWO, TrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO,
        TrucoCall.VALE_CUATRO);
    tracker.onScoreChanged(4, 0);
    tracker.onGameStarted(2);
    tracker.onRoundStarted(1, PlayerSeat.PLAYER_ONE);
    tracker.onScoreChanged(3, 0);

    final var decisions = this.policy.decideUnlocks(tracker);

    assertThat(decisions).noneMatch(
        d -> d.achievementCode() == AchievementCode.WIN_AFTER_LOSING_ACCEPTED_VALE_CUATRO_BY_BUST);
  }

  @Test
  @DisplayName("score previo incorrecto no desbloquea")
  void wrongPreviousScoreDoesNotUnlock() {

    final var tracker = this.newTracker();
    tracker.onScoreChanged(1, 0);
    tracker.onTrucoResponded(PlayerSeat.PLAYER_TWO, TrucoResponse.QUIERO, TrucoCall.RETRUCO);
    tracker.onScoreChanged(3, 0);

    final var decisions = this.policy.decideUnlocks(tracker);

    assertThat(decisions).noneMatch(
        d -> d.achievementCode() == AchievementCode.WIN_RETRUCO_FROM_0_0_TO_3);
  }

  @Test
  @DisplayName("ronda con cantos invalida logro de ganar 2-2 sin cantar")
  void callsInRoundInvalidateWinAtTwoTwoWithoutCalls() {

    final var tracker = this.newTracker();
    tracker.onScoreChanged(2, 2);
    tracker.onRoundStarted(2, PlayerSeat.PLAYER_ONE);
    tracker.onTrucoCalled(TrucoCall.TRUCO);
    tracker.onScoreChanged(3, 2);

    final var decisions = this.policy.decideUnlocks(tracker);

    assertThat(decisions).noneMatch(
        d -> d.achievementCode() == AchievementCode.WIN_AT_2_2_WITHOUT_CALLS_IN_ROUND);
  }

  private MatchAchievementTracker newTracker() {

    final var tracker = MatchAchievementTracker.create(MatchId.generate(), PlayerId.generate(),
        PlayerId.generate(), true);
    tracker.onGameStarted(1);
    tracker.onRoundStarted(1, PlayerSeat.PLAYER_ONE);
    return tracker;
  }
}
