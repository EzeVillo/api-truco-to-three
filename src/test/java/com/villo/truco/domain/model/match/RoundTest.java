package com.villo.truco.domain.model.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.model.match.events.TrucoCancelledByEnvidoEvent;
import com.villo.truco.domain.model.match.exceptions.CannotFoldWithoutCardsException;
import com.villo.truco.domain.model.match.exceptions.CardNotInHandException;
import com.villo.truco.domain.model.match.exceptions.EnvidoNotAllowedException;
import com.villo.truco.domain.model.match.exceptions.FoldNotAllowedException;
import com.villo.truco.domain.model.match.exceptions.InvalidTrucoCallException;
import com.villo.truco.domain.model.match.exceptions.NotYourTurnException;
import com.villo.truco.domain.model.match.valueobjects.AvailableAction;
import com.villo.truco.domain.model.match.valueobjects.Card;
import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.HandId;
import com.villo.truco.domain.model.match.valueobjects.RoundId;
import com.villo.truco.domain.model.match.valueobjects.RoundStatus;
import com.villo.truco.domain.model.match.valueobjects.Suit;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoundTest {

  private PlayerId mano;
  private PlayerId pie;
  private Round round;

  @BeforeEach
  void setUp() {

    this.mano = PlayerId.generate();
    this.pie = PlayerId.generate();
    this.round = Round.create(1, this.mano, this.mano, this.pie);
  }

  private void playCardFromHand(final PlayerId playerId) {

    final var card = this.round.getHandOf(playerId).getCards().getFirst();
    this.round.playCard(playerId, card);
  }

  private void playFullHand() {

    this.playCardFromHand(this.round.getCurrentTurn());
    this.playCardFromHand(this.round.getCurrentTurn());
  }

  @Test
  void manoShouldPlayFirst() {

    assertThat(this.round.getCurrentTurn()).isEqualTo(this.mano);
  }

  @Test
  void turnShouldSwitchAfterPlayingCard() {

    this.playCardFromHand(this.mano);

    assertThat(this.round.getCurrentTurn()).isEqualTo(this.pie);
  }

  @Test
  void shouldThrowWhenPlayingOutOfTurn() {

    final var card = this.round.getHandOf(this.pie).getCards().getFirst();

    assertThatThrownBy(() -> this.round.playCard(this.pie, card)).isInstanceOf(
        NotYourTurnException.class);
  }

  @Test
  void shouldThrowWhenPlayingCardNotInHand() {

    final var foreignCard = this.round.getHandOf(this.pie).getCards().getFirst();

    assertThatThrownBy(() -> this.round.playCard(this.mano, foreignCard)).isInstanceOf(
        CardNotInHandException.class);
  }

  @Test
  void roundShouldFinishWithAWinner() {

    while (this.round.getStatus() != RoundStatus.FINISHED) {
      this.playFullHand();
    }

    assertThat(this.round.getRoundWinner()).isPresent();
  }

  @Test
  void roundShouldNotBeFinishedMidGame() {

    this.playFullHand();

    assertThat(this.round.getStatus()).isNotEqualTo(RoundStatus.FINISHED);
  }

  // ===== TRUCO =====

  @Test
  void shouldChangeTurnWhenCallingTruco() {

    this.round.callTruco(this.mano);

    assertThat(this.round.getCurrentTurn()).isEqualTo(this.pie);
    assertThat(this.round.getStatus()).isEqualTo(RoundStatus.TRUCO_IN_PROGRESS);
  }

  @Test
  void shouldRestoreOriginalTurnAfterAcceptingTruco() {

    this.round.callTruco(this.mano);
    this.round.acceptTruco(this.pie);

    assertThat(this.round.getCurrentTurn()).isEqualTo(this.mano);
    assertThat(this.round.getStatus()).isEqualTo(RoundStatus.PLAYING);
  }

  @Test
  void shouldSetTwoPointsAtStakeAfterAcceptingTruco() {

    this.round.callTruco(this.mano);
    this.round.acceptTruco(this.pie);

    assertThat(this.round.getTrucoPointsAtStake()).isEqualTo(2);
  }

  @Test
  void shouldFinishRoundWithOnePointAfterRejectingTruco() {

    this.round.callTruco(this.mano);
    final var result = this.round.rejectTruco(this.pie);

    assertThat(result.points()).isEqualTo(1);
    assertThat(this.round.getStatus()).isEqualTo(RoundStatus.FINISHED);
    assertThat(result.winner()).isEqualTo(this.mano);
  }

  @Test
  void shouldAllowRetrucoAfterTruco() {

    this.round.callTruco(this.mano);
    this.round.callTruco(this.pie);

    assertThat(this.round.getStatus()).isEqualTo(RoundStatus.TRUCO_IN_PROGRESS);
    assertThat(this.round.getCurrentTurn()).isEqualTo(this.mano);
  }

  @Test
  void shouldSetThreePointsAtStakeAfterAcceptingRetruco() {

    this.round.callTruco(this.mano);
    this.round.callTruco(this.pie);
    this.round.acceptTruco(this.mano);

    assertThat(this.round.getTrucoPointsAtStake()).isEqualTo(3);
  }

  @Test
  void shouldAllowValeCuatroAfterRetruco() {

    this.round.callTruco(this.mano);
    this.round.callTruco(this.pie);
    this.round.callTruco(this.mano);

    assertThat(this.round.getStatus()).isEqualTo(RoundStatus.TRUCO_IN_PROGRESS);
  }

  @Test
  void shouldRestoreOriginalTurnAfterRetruco() {

    this.round.callTruco(this.mano);
    this.round.callTruco(this.pie);
    this.round.acceptTruco(this.mano);

    assertThat(this.round.getCurrentTurn()).isEqualTo(this.mano);
  }

  @Test
  void shouldRestoreOriginalTurnAfterFullTrucoChain() {
    // mano tiene el turno original
    this.round.callTruco(this.mano);
    this.round.callTruco(this.pie);
    this.round.callTruco(this.mano);
    this.round.acceptTruco(this.pie);

    assertThat(this.round.getCurrentTurn()).isEqualTo(this.mano);
  }

  @Test
  void shouldThrowWhenSamePlayerCallsTrucoTwice() {

    this.round.callTruco(this.mano);

    assertThatThrownBy(() -> this.round.callTruco(this.mano)).isInstanceOf(
        NotYourTurnException.class);
  }

  @Test
  void shouldThrowWhenCallingTrucoAfterValeCuatro() {

    this.round.callTruco(this.mano);
    this.round.callTruco(this.pie);
    this.round.callTruco(this.mano);

    assertThatThrownBy(() -> this.round.callTruco(this.pie)).isInstanceOf(
        InvalidTrucoCallException.class);
  }

  @Test
  void shouldFinishRoundWhenFoldingAfterTruco() {

    this.round.callTruco(this.mano);
    final var result = this.round.acceptTrucoAndFold(this.pie);

    assertThat(this.round.getStatus()).isEqualTo(RoundStatus.FINISHED);
    assertThat(result.points()).isEqualTo(2);
    assertThat(result.winner()).isEqualTo(this.mano);
  }

  @Test
  @DisplayName("no permite irse al mazo siendo mano en primera mano sin envido ni truco aceptado")
  void shouldBlockFoldForManoOnFirstHandWithoutEnvidoAndWithoutAcceptedTruco() {

    assertThatThrownBy(() -> this.round.fold(this.mano)).isInstanceOf(
        FoldNotAllowedException.class);
  }

  @Test
  @DisplayName("permite irse al mazo siendo mano si el truco ya fue aceptado")
  void shouldAllowFoldForManoAfterAcceptedTruco() {

    this.round.callTruco(this.mano);
    this.round.acceptTruco(this.pie);

    final var result = this.round.fold(this.mano);

    assertThat(this.round.getStatus()).isEqualTo(RoundStatus.FINISHED);
    assertThat(result.points()).isEqualTo(2);
    assertThat(result.winner()).isEqualTo(this.pie);
  }

  @Test
  void trucoCancelledWhenEnvidoCalled() {

    this.round.clearDomainEvents();
    this.round.callTruco(this.mano);
    this.round.callEnvido(this.pie, EnvidoCall.ENVIDO);

    assertThat(this.round.getStatus()).isEqualTo(RoundStatus.ENVIDO_IN_PROGRESS);
    assertThat(this.round.getDomainEvents()).anyMatch(
        event -> event instanceof TrucoCancelledByEnvidoEvent);
  }

  // ===== ENVIDO =====

  @Test
  void shouldChangeTurnWhenCallingEnvido() {

    this.round.callEnvido(this.mano, EnvidoCall.ENVIDO);

    assertThat(this.round.getCurrentTurn()).isEqualTo(this.pie);
    assertThat(this.round.getStatus()).isEqualTo(RoundStatus.ENVIDO_IN_PROGRESS);
  }

  @Test
  void shouldThrowWhenCallingEnvidoAfterFirstHand() {

    this.playFullHand();

    assertThatThrownBy(
        () -> this.round.callEnvido(this.round.getCurrentTurn(), EnvidoCall.ENVIDO)).isInstanceOf(
        EnvidoNotAllowedException.class);
  }

  @Test
  void shouldThrowWhenCallingEnvidoAfterPlayingCard() {

    this.playCardFromHand(this.mano);

    assertThatThrownBy(() -> this.round.callEnvido(this.mano, EnvidoCall.ENVIDO)).isInstanceOf(
        NotYourTurnException.class);
  }

  @Test
  void shouldThrowWhenCallingEnvidoAfterRetruco() {

    this.round.callTruco(this.mano);
    this.round.callTruco(this.pie);

    assertThatThrownBy(() -> this.round.callEnvido(this.mano, EnvidoCall.ENVIDO)).isInstanceOf(
        EnvidoNotAllowedException.class);
    assertThat(this.round.getStatus()).isEqualTo(RoundStatus.TRUCO_IN_PROGRESS);
    assertThat(this.round.getCurrentTrucoCall()).isEqualTo(TrucoCall.RETRUCO);
  }

  @Test
  void shouldAllowEnvidoChain() {

    this.round.callEnvido(this.mano, EnvidoCall.ENVIDO);
    this.round.callEnvido(this.pie, EnvidoCall.ENVIDO);
    this.round.callEnvido(this.mano, EnvidoCall.REAL_ENVIDO);

    assertThat(this.round.getStatus()).isEqualTo(RoundStatus.ENVIDO_IN_PROGRESS);
  }

  @Test
  @DisplayName("no se puede cantar un tercer ENVIDO")
  void shouldThrowWhenCallingThirdEnvido() {

    this.round.callEnvido(this.mano, EnvidoCall.ENVIDO);
    this.round.callEnvido(this.pie, EnvidoCall.ENVIDO);

    assertThatThrownBy(() -> this.round.callEnvido(this.mano, EnvidoCall.ENVIDO)).isInstanceOf(
        EnvidoNotAllowedException.class);
  }

  @Test
  void shouldRestoreOriginalTurnAfterRejectingEnvido() {

    this.round.callEnvido(this.mano, EnvidoCall.ENVIDO);
    this.round.rejectEnvido(this.pie);

    assertThat(this.round.getCurrentTurn()).isEqualTo(this.mano);
    assertThat(this.round.getStatus()).isEqualTo(RoundStatus.PLAYING);
  }

  @Test
  void shouldRestoreOriginalTurnAfterEnvidoChainAndRejection() {

    this.round.callEnvido(this.mano, EnvidoCall.ENVIDO);
    this.round.callEnvido(this.pie, EnvidoCall.ENVIDO);
    this.round.rejectEnvido(this.mano);

    assertThat(this.round.getCurrentTurn()).isEqualTo(this.mano);
  }

  @Test
  void shouldRestoreTurnToTrucoCallerAfterRejectingEnvidoCalledDuringTruco() {

    this.round.callTruco(this.mano);
    this.round.callEnvido(this.pie, EnvidoCall.ENVIDO);
    this.round.rejectEnvido(this.mano);

    assertThat(this.round.getStatus()).isEqualTo(RoundStatus.PLAYING);
    assertThat(this.round.getCurrentTurn()).isEqualTo(this.mano);
  }

  @Test
  void shouldRestoreTurnToTrucoCallerAfterAcceptingEnvidoCalledDuringTruco() {

    this.round.callTruco(this.mano);
    this.round.callEnvido(this.pie, EnvidoCall.ENVIDO);
    this.round.acceptEnvido(this.mano, 0, 0);

    assertThat(this.round.getStatus()).isEqualTo(RoundStatus.PLAYING);
    assertThat(this.round.getCurrentTurn()).isEqualTo(this.mano);
  }

  @Test
  void shouldReturnOnePointWhenRejectingSingleEnvido() {

    this.round.callEnvido(this.mano, EnvidoCall.REAL_ENVIDO);
    final var result = this.round.rejectEnvido(this.pie);

    assertThat(result.points()).isEqualTo(1);
  }

  @Test
  void shouldReturnSumMinusLastWhenRejectingChain() {

    this.round.callEnvido(this.mano, EnvidoCall.ENVIDO);
    this.round.callEnvido(this.pie, EnvidoCall.REAL_ENVIDO);
    final var result = this.round.rejectEnvido(this.mano);

    assertThat(result.points()).isEqualTo(2);
  }

  @Test
  void shouldReturnSumOfLongerChainMinusLast() {

    this.round.callEnvido(this.mano, EnvidoCall.ENVIDO);
    this.round.callEnvido(this.pie, EnvidoCall.ENVIDO);
    this.round.callEnvido(this.mano, EnvidoCall.REAL_ENVIDO);
    final var result = this.round.rejectEnvido(this.pie);

    assertThat(result.points()).isEqualTo(4);
  }

  @Test
  void shouldThrowWhenRaisingEnvidoWithLowerHierarchy() {

    this.round.callEnvido(this.mano, EnvidoCall.REAL_ENVIDO);

    assertThatThrownBy(() -> this.round.callEnvido(this.pie, EnvidoCall.ENVIDO)).isInstanceOf(
        EnvidoNotAllowedException.class);
  }

  @Test
  void shouldThrowWhenCallingEnvidoAfterResolved() {

    this.round.callEnvido(this.mano, EnvidoCall.ENVIDO);
    this.round.rejectEnvido(this.pie);

    assertThatThrownBy(() -> this.round.callEnvido(this.mano, EnvidoCall.ENVIDO)).isInstanceOf(
        EnvidoNotAllowedException.class);
  }

  @Test
  void shouldAllowPlayingAfterEnvidoResolved() {

    this.round.callEnvido(this.mano, EnvidoCall.ENVIDO);
    this.round.rejectEnvido(this.pie);

    assertThatNoException().isThrownBy(() -> this.playCardFromHand(this.mano));
  }

  @Test
  void shouldAllowPlayingAfterTrucoAccepted() {

    this.round.callTruco(this.mano);
    this.round.acceptTruco(this.pie);

    assertThatNoException().isThrownBy(() -> this.playCardFromHand(this.mano));
  }

  @Test
  void shouldThrowWhenCallingEnvidoAfterTrucoAccepted() {

    this.round.callTruco(this.mano);
    this.round.acceptTruco(this.pie);

    assertThatThrownBy(() -> this.round.callEnvido(this.mano, EnvidoCall.ENVIDO)).isInstanceOf(
        EnvidoNotAllowedException.class);
  }

  @Test
  @DisplayName("no es tu turno → lista vacía")
  void noActionsWhenNotYourTurn() {

    assertThat(this.round.getAvailableActions(this.pie)).isEmpty();
  }

  @Test
  @DisplayName("es tu turno siendo mano en primera mano sin envido/truco aceptado → sin FOLD")
  void allActionsAvailableOnFirstHandWithTurn() {

    final var actions = this.round.getAvailableActions(this.mano);

    assertThat(actionsOfType(actions, "PLAY_CARD")).isEmpty(); // sin parámetros
    assertThat(hasActionType(actions, "FOLD")).isFalse();
    assertThat(actionsOfType(actions, "CALL_TRUCO")).containsExactly("TRUCO");
    assertThat(actionsOfType(actions, "CALL_ENVIDO")).containsExactlyInAnyOrder("ENVIDO",
        "REAL_ENVIDO", "FALTA_ENVIDO");
  }

  @Test
  @DisplayName("ya jugaste carta en la mano actual → no puede cantar envido")
  void noEnvidoAfterPlayingCard() {

    this.playCardFromHand(this.mano);
    final var actions = this.round.getAvailableActions(this.mano);

    assertThat(hasActionType(actions, "CALL_ENVIDO")).isFalse();
  }

  @Test
  @DisplayName("segunda mano → no puede cantar envido")
  void noEnvidoOnSecondHand() {

    this.playFullHand();
    // arrancó segunda mano
    final var actions = this.round.getAvailableActions(this.round.getCurrentTurn());

    assertThat(hasActionType(actions, "CALL_ENVIDO")).isFalse();
  }

  @Test
  @DisplayName("envido ya resuelto → no aparece CALL_ENVIDO")
  void noEnvidoAfterResolved() {

    this.round.callEnvido(this.mano, EnvidoCall.ENVIDO);
    this.round.rejectEnvido(this.pie);

    final var actions = this.round.getAvailableActions(this.mano);

    assertThat(hasActionType(actions, "CALL_ENVIDO")).isFalse();
  }

  @Test
  @DisplayName("rival cantó truco → puede subir a RETRUCO")
  void canRaiseTrucoAfterRivalCalled() {

    this.round.callTruco(this.mano);
    // ahora es turno de pie para responder
    final var actions = this.round.getAvailableActions(this.pie);

    assertThat(actionsOfType(actions, "CALL_TRUCO")).containsExactly("RETRUCO");
  }

  @Test
  @DisplayName("estado TRUCO_IN_PROGRESS → RESPOND_TRUCO(QUIERO, NO_QUIERO, QUIERO_Y_ME_VOY_AL_MAZO) y puede subir")
  void respondTrucoActionsWhenTrucoInProgress() {

    this.round.callTruco(this.mano);
    final var actions = this.round.getAvailableActions(this.pie);

    assertThat(actionsOfType(actions, "RESPOND_TRUCO")).containsExactlyInAnyOrder("QUIERO",
        "NO_QUIERO", "QUIERO_Y_ME_VOY_AL_MAZO");
    assertThat(actionsOfType(actions, "CALL_TRUCO")).containsExactly("RETRUCO");
    assertThat(actionsOfType(actions, "CALL_ENVIDO")).containsExactlyInAnyOrder("ENVIDO",
        "REAL_ENVIDO", "FALTA_ENVIDO");
  }

  @Test
  @DisplayName("estado TRUCO_IN_PROGRESS con RETRUCO → no aparece CALL_ENVIDO")
  void noEnvidoActionsWhenRetrucoInProgress() {

    this.round.callTruco(this.mano);
    this.round.callTruco(this.pie);

    final var actions = this.round.getAvailableActions(this.mano);

    assertThat(hasActionType(actions, "CALL_ENVIDO")).isFalse();
  }

  @Test
  @DisplayName("respondiendo truco y habiendo jugado carta → no puede cantar envido")
  void noEnvidoWhenRespondingTrucoAfterPlayingCard() {

    this.playCardFromHand(this.mano);
    this.round.callTruco(this.pie);

    final var actions = this.round.getAvailableActions(this.mano);

    assertThat(hasActionType(actions, "CALL_ENVIDO")).isFalse();
    assertThatThrownBy(() -> this.round.callEnvido(this.mano, EnvidoCall.ENVIDO)).isInstanceOf(
        EnvidoNotAllowedException.class);
  }

  @Test
  @DisplayName("truco aceptado y sin cartas jugadas → no aparece CALL_ENVIDO")
  void noEnvidoActionsAfterTrucoAccepted() {

    this.round.callTruco(this.mano);
    this.round.acceptTruco(this.pie);

    final var actions = this.round.getAvailableActions(this.mano);

    assertThat(hasActionType(actions, "CALL_ENVIDO")).isFalse();
    assertThat(hasActionType(actions, "FOLD")).isTrue();
  }

  @Test
  @DisplayName("VALE_CUATRO en progreso → no puede subir más")
  void noRaiseAfterValeCuatro() {

    this.round.callTruco(this.mano);  // TRUCO
    this.round.callTruco(this.pie);   // RETRUCO
    this.round.callTruco(this.mano);  // VALE_CUATRO

    final var actions = this.round.getAvailableActions(this.pie);

    assertThat(actionsOfType(actions, "RESPOND_TRUCO")).containsExactlyInAnyOrder("QUIERO",
        "NO_QUIERO", "QUIERO_Y_ME_VOY_AL_MAZO");
    assertThat(hasActionType(actions, "CALL_TRUCO")).isFalse();
  }

  @Test
  @DisplayName("estado ENVIDO_IN_PROGRESS → RESPOND_ENVIDO(QUIERO, NO_QUIERO) y puede subir")
  void respondEnvidoActionsWhenEnvidoInProgress() {

    this.round.callEnvido(this.mano, EnvidoCall.ENVIDO);
    final var actions = this.round.getAvailableActions(this.pie);

    assertThat(actionsOfType(actions, "RESPOND_ENVIDO")).containsExactlyInAnyOrder("QUIERO",
        "NO_QUIERO");
    assertThat(actionsOfType(actions, "CALL_ENVIDO")).containsExactlyInAnyOrder("ENVIDO",
        "REAL_ENVIDO", "FALTA_ENVIDO");
  }

  @Test
  @DisplayName("FALTA_ENVIDO en progreso → no puede subir más")
  void noRaiseAfterFaltaEnvido() {

    this.round.callEnvido(this.mano, EnvidoCall.FALTA_ENVIDO);
    final var actions = this.round.getAvailableActions(this.pie);

    assertThat(actionsOfType(actions, "RESPOND_ENVIDO")).containsExactlyInAnyOrder("QUIERO",
        "NO_QUIERO");
    assertThat(hasActionType(actions, "CALL_ENVIDO")).isFalse();
  }

  @Test
  @DisplayName("ronda terminada → lista vacía para ambos jugadores")
  void noActionsWhenRoundFinished() {

    this.round.callTruco(this.mano);
    this.round.rejectTruco(this.pie);

    assertThat(this.round.getAvailableActions(this.mano)).isEmpty();
    assertThat(this.round.getAvailableActions(this.pie)).isEmpty();
  }

  @Test
  @DisplayName("sin cartas jugadas → currentHandInfo vacío y playedHands vacío")
  void noCardsPlayedInitially() {

    final var handInfo = this.round.getCurrentHandInfo();
    assertThat(handInfo.cardPlayerOne()).isNull();
    assertThat(handInfo.cardPlayerTwo()).isNull();
    assertThat(this.round.getPlayedHands()).isEmpty();
  }

  @Test
  @DisplayName("después de dos cartas → currentHandInfo vacío y playedHands tiene la mano")
  void handResolvedAfterTwoCards() {

    final var cardMano = this.round.getHandOf(this.mano).getCards().getFirst();
    final var cardPie = this.round.getHandOf(this.pie).getCards().getFirst();

    this.round.playCard(this.mano, cardMano);
    this.round.playCard(this.pie, cardPie);

    final var handInfo = this.round.getCurrentHandInfo();
    assertThat(handInfo.cardPlayerOne()).isNull();
    assertThat(handInfo.cardPlayerTwo()).isNull();

    assertThat(this.round.getPlayedHands()).hasSize(1);

    final var playedHand = this.round.getPlayedHands().getFirst();
    // mano es playerOne en el setUp — su carta es cardPlayerOne
    assertThat(playedHand.cardPlayerOne()).isEqualTo(cardMano);
    assertThat(playedHand.cardPlayerTwo()).isEqualTo(cardPie);
  }

  @Test
  @DisplayName("currentHandInfo refleja quién jugó en la mano actual")
  void currentHandInfoAfterOneCard() {

    final var card = this.round.getHandOf(this.mano).getCards().getFirst();
    this.round.playCard(this.mano, card);

    final var handInfo = this.round.getCurrentHandInfo();
    // mano es playerOne — jugó la primera carta
    assertThat(handInfo.cardPlayerOne()).isEqualTo(card);
    assertThat(handInfo.cardPlayerTwo()).isNull();
  }

  @Test
  @DisplayName("currentHandInfo del pie después de que jugó")
  void currentHandInfoAfterPiePlays() {

    this.playCardFromHand(this.mano);
    final var card = this.round.getHandOf(this.pie).getCards().getFirst();
    this.round.playCard(this.pie, card);

    // ambos jugaron — se resolvió la mano, currentHandInfo vacío
    final var handInfo = this.round.getCurrentHandInfo();
    assertThat(handInfo.cardPlayerOne()).isNull();
    assertThat(handInfo.cardPlayerTwo()).isNull();
  }

  @Test
  @DisplayName("playedHands registra el ganador de la mano")
  void playedHandRecordsWinner() {

    this.playFullHand();

    final var playedHand = this.round.getPlayedHands().getFirst();
    assertThat(playedHand.winner()).satisfiesAnyOf(w -> assertThat(w).isNull(),
        w -> assertThat(w).isIn(this.mano, this.pie));
  }

  @Test
  @DisplayName("segunda mano → playedHands tiene dos entradas")
  void secondHandAddsToPlayedHands() {

    this.playFullHand();
    this.playFullHand();

    assertThat(this.round.getPlayedHands()).hasSize(2);
  }

  @Test
  @DisplayName("getManoPlayer devuelve el mano de la ronda")
  void getManoPlayerReturnsMano() {

    assertThat(this.round.getManoPlayer()).isEqualTo(this.mano);
  }

  @Test
  @DisplayName("acceptTrucoAndFold lanza excepción si el jugador no tiene cartas")
  void shouldThrowWhenAcceptingTrucoAndFoldWithNoCards() {

    final var roundNoCards = createRoundWithTrucoInProgressAndResponderHasNoCards();

    assertThatThrownBy(() -> roundNoCards.acceptTrucoAndFold(this.pie)).isInstanceOf(
        CannotFoldWithoutCardsException.class);
  }

  @Test
  @DisplayName("sin cartas → QUIERO_Y_ME_VOY_AL_MAZO no aparece en acciones disponibles")
  void shouldNotOfferFoldWhenRespondingTrucoWithNoCards() {

    final var roundNoCards = createRoundWithTrucoInProgressAndResponderHasNoCards();

    final var actions = roundNoCards.getAvailableActions(this.pie);

    assertThat(actionsOfType(actions, "RESPOND_TRUCO")).containsExactlyInAnyOrder("QUIERO",
        "NO_QUIERO");
  }

  @Test
  @DisplayName("con cartas → QUIERO_Y_ME_VOY_AL_MAZO sí aparece en acciones disponibles")
  void shouldOfferFoldWhenRespondingTrucoWithCards() {

    this.round.callTruco(this.mano);

    final var actions = this.round.getAvailableActions(this.pie);

    assertThat(actionsOfType(actions, "RESPOND_TRUCO")).contains("QUIERO_Y_ME_VOY_AL_MAZO");
  }

  private Round createRoundWithTrucoInProgressAndResponderHasNoCards() {

    final var callerCard = Card.of(Suit.ESPADA, 1);
    final var callerHand = Hand.reconstruct(HandId.generate(), List.of(callerCard));
    final var responderHand = Hand.reconstruct(HandId.generate(), List.of());

    final var roundNoCards = Round.reconstruct(RoundId.generate(), 1, this.mano, this.mano,
        this.pie, callerHand, responderHand, List.of(), List.of(), RoundStatus.TRUCO_IN_PROGRESS,
        this.pie, this.mano, null);

    roundNoCards.getTrucoStateMachine().initializeState(TrucoCall.TRUCO, this.mano, 1);

    return roundNoCards;
  }

  private List<String> actionsOfType(final List<AvailableAction> actions, final String type) {

    return actions.stream().filter(a -> a.type().name().equals(type))
        .map(a -> a.getParameter().orElse(null)).filter(Objects::nonNull).toList();
  }

  private boolean hasActionType(final List<AvailableAction> actions, final String type) {

    return actions.stream().anyMatch(a -> a.type().name().equals(type));
  }

}