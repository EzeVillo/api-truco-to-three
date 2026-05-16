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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("AchievementPolicy")
class AchievementPolicyTest {

  private final AchievementPolicy policy = new AchievementPolicy();

  @Test
  @DisplayName("desbloquea WIN_MATCH_AS_PIE_MANO_BUSTS_ON_ENVIDO_WITH_0_0_AT_2_2")
  void unlocksWinMatchAsPieManoOBustsOnEnvidoAt2_2() {

    final var tracker = this.newTracker();
    tracker.onScoreChanged(2, 2);
    tracker.onRoundStarted(PlayerSeat.PLAYER_ONE);
    tracker.onEnvidoCalled(EnvidoCall.ENVIDO);
    tracker.onEnvidoResolved(EnvidoResponse.QUIERO, PlayerSeat.PLAYER_ONE, 0, 0);
    tracker.onScoreChanged(4, 2);

    final var decisions = this.policy.decideUnlocks(tracker);

    assertThat(decisions).contains(
        new AchievementUnlockDecision(tracker.getPlayerTwo(),
            AchievementCode.WIN_MATCH_AS_PIE_MANO_BUSTS_ON_ENVIDO_WITH_0_0_AT_2_2));
    assertThat(decisions).doesNotContain(
        new AchievementUnlockDecision(tracker.getPlayerOne(),
            AchievementCode.WIN_MATCH_AS_PIE_MANO_BUSTS_ON_ENVIDO_WITH_0_0_AT_2_2));
  }

  @Test
  @DisplayName("desbloquea WIN_MATCH_AS_MANO_VIA_FALTA_ENVIDO_WITH_33_33_AT_2_2")
  void unlocksWinMatchAsManoViaFaltaEnvidoWith33At2_2() {

    final var tracker = this.newTracker();
    tracker.onScoreChanged(2, 2);
    tracker.onRoundStarted(PlayerSeat.PLAYER_ONE);
    tracker.onEnvidoCalled(EnvidoCall.FALTA_ENVIDO);
    tracker.onEnvidoResolved(EnvidoResponse.QUIERO, PlayerSeat.PLAYER_ONE, 33, 33);
    tracker.onScoreChanged(3, 2);

    final var decisions = this.policy.decideUnlocks(tracker);

    assertThat(decisions).contains(
        new AchievementUnlockDecision(tracker.getPlayerOne(),
            AchievementCode.WIN_MATCH_AS_MANO_VIA_FALTA_ENVIDO_WITH_33_33_AT_2_2));
    assertThat(decisions).doesNotContain(
        new AchievementUnlockDecision(tracker.getPlayerTwo(),
            AchievementCode.WIN_MATCH_AS_MANO_VIA_FALTA_ENVIDO_WITH_33_33_AT_2_2));
  }

  @Test
  @DisplayName("desbloquea WIN_MATCH_BUST_OPPONENT_VIA_QUIERO_Y_ME_VOY_AL_MAZO")
  void unlocksWinMatchBustOpponentViaQuieroYMeVoyAlMazo() {

    final var tracker = this.newTracker();
    tracker.onScoreChanged(2, 0);
    tracker.onRoundStarted(PlayerSeat.PLAYER_ONE);
    tracker.onTrucoResponded(PlayerSeat.PLAYER_TWO, TrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO,
        TrucoCall.TRUCO);
    tracker.onScoreChanged(4, 0);

    final var decisions = this.policy.decideUnlocks(tracker);

    assertThat(decisions).contains(
        new AchievementUnlockDecision(tracker.getPlayerTwo(),
            AchievementCode.WIN_MATCH_BUST_OPPONENT_VIA_QUIERO_Y_ME_VOY_AL_MAZO));
    assertThat(decisions).doesNotContain(
        new AchievementUnlockDecision(tracker.getPlayerOne(),
            AchievementCode.WIN_MATCH_BUST_OPPONENT_VIA_QUIERO_Y_ME_VOY_AL_MAZO));
  }

  @Test
  @DisplayName("desbloquea WIN_HAND_UNCONTESTED_WITH_ANCHO_DE_ESPADA")
  void unlocksWinHandUncontestedWithAnchoDeEspada() {

    final var tracker = this.newTracker();
    tracker.onHandResolved(Card.of(Suit.ESPADA, 1), null, PlayerSeat.PLAYER_ONE);

    final var decisions = this.policy.decideUnlocks(tracker);

    assertThat(decisions).contains(
        new AchievementUnlockDecision(tracker.getPlayerOne(),
            AchievementCode.WIN_HAND_UNCONTESTED_WITH_ANCHO_DE_ESPADA));
    assertThat(decisions).doesNotContain(
        new AchievementUnlockDecision(tracker.getPlayerTwo(),
            AchievementCode.WIN_HAND_UNCONTESTED_WITH_ANCHO_DE_ESPADA));
  }

  @Test
  @DisplayName("desbloquea FOLD_BEFORE_ANY_CARD_IS_PLAYED")
  void unlocksFoldBeforePlayingFirstHand() {

    final var tracker = this.newTracker();
    tracker.onFolded(PlayerSeat.PLAYER_TWO);

    final var decisions = this.policy.decideUnlocks(tracker);

    assertThat(decisions).contains(
        new AchievementUnlockDecision(tracker.getPlayerTwo(),
            AchievementCode.FOLD_BEFORE_ANY_CARD_IS_PLAYED));
    assertThat(decisions).doesNotContain(
        new AchievementUnlockDecision(tracker.getPlayerOne(),
            AchievementCode.FOLD_BEFORE_ANY_CARD_IS_PLAYED));
  }

  @Test
  @DisplayName("mano juega carta y pie no desbloquea FOLD_BEFORE_ANY_CARD_IS_PLAYED")
  void manoPlaysCardBeforePieFoldsDoesNotUnlockFoldBeforePlayingFirstHand() {

    final var tracker = this.newTracker();
    tracker.onCardPlayed(PlayerSeat.PLAYER_ONE);
    tracker.onFolded(PlayerSeat.PLAYER_TWO);

    assertThat(this.policy.decideUnlocks(tracker)).noneMatch(
        d -> d.achievementCode() == AchievementCode.FOLD_BEFORE_ANY_CARD_IS_PLAYED);
  }

  @Test
  @DisplayName("desbloquea WIN_MATCH_THREE_ZERO_VIA_ACCEPTED_RETRUCO")
  void unlocksWinMatchThreeZeroViaAcceptedRetruco() {

    final var tracker = this.newTracker();
    tracker.onTrucoResponded(PlayerSeat.PLAYER_TWO, TrucoResponse.QUIERO, TrucoCall.RETRUCO);
    tracker.onScoreChanged(3, 0);

    final var decisions = this.policy.decideUnlocks(tracker);

    assertThat(decisions).contains(
        new AchievementUnlockDecision(tracker.getPlayerOne(),
            AchievementCode.WIN_MATCH_THREE_ZERO_VIA_ACCEPTED_RETRUCO));
    assertThat(decisions).doesNotContain(
        new AchievementUnlockDecision(tracker.getPlayerTwo(),
            AchievementCode.WIN_MATCH_THREE_ZERO_VIA_ACCEPTED_RETRUCO));
  }

  @ParameterizedTest(name = "con {0}")
  @EnumSource(value = EnvidoCall.class, names = {"REAL_ENVIDO", "FALTA_ENVIDO"})
  @DisplayName("desbloquea WIN_MATCH_THREE_ZERO_VIA_REAL_OR_FALTA_ENVIDO")
  void unlocksWinMatchThreeZeroViaRealOrFaltaEnvido(final EnvidoCall call) {

    final var tracker = this.newTracker();
    tracker.onEnvidoCalled(call);
    tracker.onEnvidoResolved(EnvidoResponse.QUIERO, PlayerSeat.PLAYER_TWO, 29, 31);
    tracker.onScoreChanged(0, 3);

    final var decisions = this.policy.decideUnlocks(tracker);

    assertThat(decisions).contains(
        new AchievementUnlockDecision(tracker.getPlayerTwo(),
            AchievementCode.WIN_MATCH_THREE_ZERO_VIA_REAL_OR_FALTA_ENVIDO));
    assertThat(decisions).doesNotContain(
        new AchievementUnlockDecision(tracker.getPlayerOne(),
            AchievementCode.WIN_MATCH_THREE_ZERO_VIA_REAL_OR_FALTA_ENVIDO));
  }

  @Test
  @DisplayName("desbloquea WIN_MATCH_FROM_2_2_WITHOUT_CALLS_IN_ROUND")
  void unlocksWinMatchFrom2_2WithoutCallsInRound() {

    final var tracker = this.newTracker();
    tracker.onScoreChanged(2, 2);
    tracker.onRoundStarted(PlayerSeat.PLAYER_TWO);
    tracker.onScoreChanged(3, 2);

    final var decisions = this.policy.decideUnlocks(tracker);

    assertThat(decisions).contains(
        new AchievementUnlockDecision(tracker.getPlayerOne(),
            AchievementCode.WIN_MATCH_FROM_2_2_WITHOUT_CALLS_IN_ROUND));
    assertThat(decisions).doesNotContain(
        new AchievementUnlockDecision(tracker.getPlayerTwo(),
            AchievementCode.WIN_MATCH_FROM_2_2_WITHOUT_CALLS_IN_ROUND));
  }

  @Test
  @DisplayName("desbloquea WIN_MATCH_BUST_OPPONENT_VIA_VALE_CUATRO_LOSS_AT_0_0")
  void unlocksWinMatchBustOpponentViaValeCuatroLossAt0_0() {

    final var tracker = this.newTracker();
    tracker.onTrucoResponded(PlayerSeat.PLAYER_TWO, TrucoResponse.QUIERO, TrucoCall.VALE_CUATRO);
    tracker.onScoreChanged(4, 0);

    final var decisions = this.policy.decideUnlocks(tracker);

    assertThat(decisions).contains(
        new AchievementUnlockDecision(tracker.getPlayerTwo(),
            AchievementCode.WIN_MATCH_BUST_OPPONENT_VIA_VALE_CUATRO_LOSS_AT_0_0));
    assertThat(decisions).doesNotContain(
        new AchievementUnlockDecision(tracker.getPlayerOne(),
            AchievementCode.WIN_MATCH_BUST_OPPONENT_VIA_VALE_CUATRO_LOSS_AT_0_0));
  }

  @Test
  @DisplayName("desbloquea WIN_MATCH_BUST_OPPONENT_VIA_VALE_CUATRO_LOSS_AT_0_0 cuando el que dijo QUIERO se pasa")
  void unlocksWinMatchBustOpponentViaValeCuatroLossAt0_0WhenResponderBusts() {

    final var tracker = this.newTracker();
    tracker.onTrucoResponded(PlayerSeat.PLAYER_TWO, TrucoResponse.QUIERO, TrucoCall.VALE_CUATRO);
    tracker.onScoreChanged(0, 4);

    final var decisions = this.policy.decideUnlocks(tracker);

    assertThat(decisions).contains(
        new AchievementUnlockDecision(tracker.getPlayerOne(),
            AchievementCode.WIN_MATCH_BUST_OPPONENT_VIA_VALE_CUATRO_LOSS_AT_0_0));
    assertThat(decisions).doesNotContain(
        new AchievementUnlockDecision(tracker.getPlayerTwo(),
            AchievementCode.WIN_MATCH_BUST_OPPONENT_VIA_VALE_CUATRO_LOSS_AT_0_0));
  }

  @Test
  @DisplayName("no desbloquea WIN_MATCH_BUST_OPPONENT_VIA_VALE_CUATRO_LOSS_AT_0_0 si no iban 0 a 0")
  void doesNotUnlockWinMatchBustOpponentViaValeCuatroLossAt0_0WhenScoreIsNot0_0() {

    final var tracker = this.newTracker();
    tracker.onScoreChanged(1, 0);
    tracker.onRoundStarted(PlayerSeat.PLAYER_ONE);
    tracker.onTrucoResponded(PlayerSeat.PLAYER_TWO, TrucoResponse.QUIERO, TrucoCall.VALE_CUATRO);
    tracker.onScoreChanged(5, 0);

    assertThat(this.policy.decideUnlocks(tracker)).noneMatch(d -> d.achievementCode()
        == AchievementCode.WIN_MATCH_BUST_OPPONENT_VIA_VALE_CUATRO_LOSS_AT_0_0);
  }

  @Test
  @DisplayName("33 vs 32 no desbloquea WIN_MATCH_AS_MANO_VIA_FALTA_ENVIDO_WITH_33_33_AT_2_2")
  void manoWith33PieWith32DoesNotUnlockFaltaEnvidoAt2_2() {

    final var tracker = this.newTracker();
    tracker.onScoreChanged(2, 2);
    tracker.onRoundStarted(PlayerSeat.PLAYER_ONE);
    tracker.onEnvidoCalled(EnvidoCall.FALTA_ENVIDO);
    tracker.onEnvidoResolved(EnvidoResponse.QUIERO, PlayerSeat.PLAYER_ONE, 33, 32);

    assertThat(this.policy.decideUnlocks(tracker)).noneMatch(d -> d.achievementCode()
        == AchievementCode.WIN_MATCH_AS_MANO_VIA_FALTA_ENVIDO_WITH_33_33_AT_2_2);
  }

  @ParameterizedTest(name = "con {0}")
  @EnumSource(value = EnvidoCall.class, names = {"REAL_ENVIDO", "FALTA_ENVIDO"})
  @DisplayName("NO_QUIERO no desbloquea logros que requieren QUIERO")
  void noQuieroDoesNotUnlockAchievementsThatRequireQuiero(final EnvidoCall call) {

    final var tracker = this.newTracker();
    tracker.onEnvidoCalled(call);
    tracker.onEnvidoResolved(EnvidoResponse.NO_QUIERO, PlayerSeat.PLAYER_ONE, null, null);

    final var decisions = this.policy.decideUnlocks(tracker);

    assertThat(decisions).noneMatch(
        d -> d.achievementCode() == AchievementCode.WIN_MATCH_THREE_ZERO_VIA_REAL_OR_FALTA_ENVIDO);
  }

  @Test
  @DisplayName("QUIERO_Y_ME_VOY_AL_MAZO no desbloquea logro de perder vale cuatro")
  void quieroYMeVoyAlMazoDoesNotUnlockValeCuatroLoss() {

    final var tracker = this.newTracker();
    tracker.onTrucoResponded(PlayerSeat.PLAYER_TWO, TrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO,
        TrucoCall.VALE_CUATRO);
    tracker.onScoreChanged(4, 0);

    assertThat(this.policy.decideUnlocks(tracker)).noneMatch(d -> d.achievementCode()
        == AchievementCode.WIN_MATCH_BUST_OPPONENT_VIA_VALE_CUATRO_LOSS_AT_0_0);
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
        d -> d.achievementCode() == AchievementCode.WIN_MATCH_THREE_ZERO_VIA_ACCEPTED_RETRUCO);
  }

  @Test
  @DisplayName("ronda con cantos invalida logro de ganar 2-2 sin cantar")
  void callsInRoundInvalidateWinAt2_2WithoutCalls() {

    final var tracker = this.newTracker();
    tracker.onScoreChanged(2, 2);
    tracker.onRoundStarted(PlayerSeat.PLAYER_ONE);
    tracker.onTrucoCalled(PlayerSeat.PLAYER_ONE);
    tracker.onScoreChanged(3, 2);

    final var decisions = this.policy.decideUnlocks(tracker);

    assertThat(decisions).noneMatch(
        d -> d.achievementCode() == AchievementCode.WIN_MATCH_FROM_2_2_WITHOUT_CALLS_IN_ROUND);
  }

  @Test
  @DisplayName("desbloquea WIN_MATCH_BUST_RIVAL_VIA_FOLD_AFTER_ACCEPTED_TRUCO_WITH_NO_CARDS (TRUCO, score 2-2)")
  void unlocksBustRivalViaFoldAfterAcceptedTrucoWithNoCards() {

    final var tracker = this.newTracker();
    tracker.onScoreChanged(2, 2);
    tracker.onRoundStarted(PlayerSeat.PLAYER_ONE);
    tracker.onCardPlayed(PlayerSeat.PLAYER_ONE);
    tracker.onCardPlayed(PlayerSeat.PLAYER_TWO);
    tracker.onHandResolved(null, null, PlayerSeat.PLAYER_ONE);
    tracker.onCardPlayed(PlayerSeat.PLAYER_ONE);
    tracker.onCardPlayed(PlayerSeat.PLAYER_TWO);
    tracker.onHandResolved(null, null, PlayerSeat.PLAYER_ONE);
    tracker.onCardPlayed(PlayerSeat.PLAYER_TWO);
    tracker.onTrucoCalled(PlayerSeat.PLAYER_ONE);
    tracker.onTrucoResponded(PlayerSeat.PLAYER_TWO, TrucoResponse.QUIERO, TrucoCall.TRUCO);
    tracker.onFolded(PlayerSeat.PLAYER_ONE);
    tracker.onScoreChanged(2, 4);

    final var decisions = this.policy.decideUnlocks(tracker);

    assertThat(decisions).contains(
        new AchievementUnlockDecision(tracker.getPlayerOne(),
            AchievementCode.WIN_MATCH_BUST_RIVAL_VIA_FOLD_AFTER_ACCEPTED_TRUCO_WITH_NO_CARDS));
    assertThat(decisions).doesNotContain(
        new AchievementUnlockDecision(tracker.getPlayerTwo(),
            AchievementCode.WIN_MATCH_BUST_RIVAL_VIA_FOLD_AFTER_ACCEPTED_TRUCO_WITH_NO_CARDS));
  }

  @ParameterizedTest(name = "con {0}")
  @EnumSource(value = TrucoCall.class, names = {"RETRUCO", "VALE_CUATRO"})
  @DisplayName("desbloquea WIN_MATCH_BUST_RIVAL_VIA_FOLD con subida de truco")
  void unlocksBustRivalViaFoldAfterAcceptedTrucoWithNoCardsWhenRaising(final TrucoCall call) {

    final var tracker = this.newTracker();
    tracker.onScoreChanged(1, 2);
    tracker.onRoundStarted(PlayerSeat.PLAYER_ONE);
    tracker.onCardPlayed(PlayerSeat.PLAYER_ONE);
    tracker.onCardPlayed(PlayerSeat.PLAYER_TWO);
    tracker.onHandResolved(null, null, PlayerSeat.PLAYER_ONE);
    tracker.onCardPlayed(PlayerSeat.PLAYER_ONE);
    tracker.onCardPlayed(PlayerSeat.PLAYER_TWO);
    tracker.onHandResolved(null, null, PlayerSeat.PLAYER_ONE);
    tracker.onCardPlayed(PlayerSeat.PLAYER_TWO);
    tracker.onTrucoCalled(PlayerSeat.PLAYER_ONE);
    tracker.onTrucoResponded(PlayerSeat.PLAYER_TWO, TrucoResponse.QUIERO, call);
    tracker.onFolded(PlayerSeat.PLAYER_ONE);
    tracker.onScoreChanged(1, call.pointsIfAccepted() + 2);

    final var decisions = this.policy.decideUnlocks(tracker);

    assertThat(decisions).contains(
        new AchievementUnlockDecision(tracker.getPlayerOne(),
            AchievementCode.WIN_MATCH_BUST_RIVAL_VIA_FOLD_AFTER_ACCEPTED_TRUCO_WITH_NO_CARDS));
    assertThat(decisions).doesNotContain(
        new AchievementUnlockDecision(tracker.getPlayerTwo(),
            AchievementCode.WIN_MATCH_BUST_RIVAL_VIA_FOLD_AFTER_ACCEPTED_TRUCO_WITH_NO_CARDS));
  }

  @Test
  @DisplayName("no desbloquea si el rival si tenia cartas al cantarse truco")
  void doesNotUnlockBustRivalViaFoldWhenRivalStillHadCards() {

    final var tracker = this.newTracker();
    tracker.onScoreChanged(2, 2);
    tracker.onRoundStarted(PlayerSeat.PLAYER_ONE);
    tracker.onCardPlayed(PlayerSeat.PLAYER_TWO);
    tracker.onTrucoCalled(PlayerSeat.PLAYER_ONE);
    tracker.onTrucoResponded(PlayerSeat.PLAYER_TWO, TrucoResponse.QUIERO, TrucoCall.TRUCO);
    tracker.onFolded(PlayerSeat.PLAYER_ONE);
    tracker.onScoreChanged(2, 4);

    assertThat(this.policy.decideUnlocks(tracker)).noneMatch(d -> d.achievementCode()
        == AchievementCode.WIN_MATCH_BUST_RIVAL_VIA_FOLD_AFTER_ACCEPTED_TRUCO_WITH_NO_CARDS);
  }

  @Test
  @DisplayName("no desbloquea si el rival respondia NO_QUIERO")
  void doesNotUnlockBustRivalViaFoldWhenRivalSaidNoQuiero() {

    final var tracker = this.newTracker();
    tracker.onScoreChanged(2, 2);
    tracker.onRoundStarted(PlayerSeat.PLAYER_ONE);
    tracker.onCardPlayed(PlayerSeat.PLAYER_ONE);
    tracker.onCardPlayed(PlayerSeat.PLAYER_TWO);
    tracker.onHandResolved(null, null, PlayerSeat.PLAYER_ONE);
    tracker.onCardPlayed(PlayerSeat.PLAYER_ONE);
    tracker.onCardPlayed(PlayerSeat.PLAYER_TWO);
    tracker.onHandResolved(null, null, PlayerSeat.PLAYER_ONE);
    tracker.onCardPlayed(PlayerSeat.PLAYER_TWO);
    tracker.onTrucoCalled(PlayerSeat.PLAYER_ONE);
    tracker.onTrucoResponded(PlayerSeat.PLAYER_TWO, TrucoResponse.NO_QUIERO, TrucoCall.TRUCO);
    tracker.onScoreChanged(2, 3);

    assertThat(this.policy.decideUnlocks(tracker)).noneMatch(d -> d.achievementCode()
        == AchievementCode.WIN_MATCH_BUST_RIVAL_VIA_FOLD_AFTER_ACCEPTED_TRUCO_WITH_NO_CARDS);
  }

  @Test
  @DisplayName("no desbloquea si vos no te fuiste al mazo")
  void doesNotUnlockBustRivalViaFoldWhenCallerDidNotFold() {

    final var tracker = this.newTracker();
    tracker.onScoreChanged(2, 2);
    tracker.onRoundStarted(PlayerSeat.PLAYER_ONE);
    tracker.onCardPlayed(PlayerSeat.PLAYER_ONE);
    tracker.onCardPlayed(PlayerSeat.PLAYER_TWO);
    tracker.onHandResolved(null, null, PlayerSeat.PLAYER_ONE);
    tracker.onCardPlayed(PlayerSeat.PLAYER_ONE);
    tracker.onCardPlayed(PlayerSeat.PLAYER_TWO);
    tracker.onHandResolved(null, null, PlayerSeat.PLAYER_ONE);
    tracker.onCardPlayed(PlayerSeat.PLAYER_TWO);
    tracker.onTrucoCalled(PlayerSeat.PLAYER_ONE);
    tracker.onTrucoResponded(PlayerSeat.PLAYER_TWO, TrucoResponse.QUIERO, TrucoCall.TRUCO);
    tracker.onScoreChanged(2, 4);

    assertThat(this.policy.decideUnlocks(tracker)).noneMatch(d -> d.achievementCode()
        == AchievementCode.WIN_MATCH_BUST_RIVAL_VIA_FOLD_AFTER_ACCEPTED_TRUCO_WITH_NO_CARDS);
  }

  @Test
  @DisplayName("logros WIN_MATCH de envido no se desbloquean en ENVIDO_RESOLVED, solo en SCORE_CHANGED")
  void envidoMatchAchievementsOnlyUnlockOnScoreChanged() {

    final var tracker = this.newTracker();
    tracker.onEnvidoCalled(EnvidoCall.REAL_ENVIDO);
    tracker.onEnvidoResolved(EnvidoResponse.QUIERO, PlayerSeat.PLAYER_TWO, 29, 31);

    assertThat(this.policy.decideUnlocks(tracker)).noneMatch(
        d -> d.achievementCode() == AchievementCode.WIN_MATCH_THREE_ZERO_VIA_REAL_OR_FALTA_ENVIDO);

    tracker.onScoreChanged(0, 3);

    final var decisions = this.policy.decideUnlocks(tracker);

    assertThat(decisions).contains(
        new AchievementUnlockDecision(tracker.getPlayerTwo(),
            AchievementCode.WIN_MATCH_THREE_ZERO_VIA_REAL_OR_FALTA_ENVIDO));
    assertThat(decisions).doesNotContain(
        new AchievementUnlockDecision(tracker.getPlayerOne(),
            AchievementCode.WIN_MATCH_THREE_ZERO_VIA_REAL_OR_FALTA_ENVIDO));
  }

  private MatchAchievementTracker newTracker() {

    final var tracker = MatchAchievementTracker.create(MatchId.generate(), PlayerId.generate(),
        PlayerId.generate());
    tracker.onGameStarted(1);
    tracker.onRoundStarted(PlayerSeat.PLAYER_ONE);
    return tracker;
  }

}

