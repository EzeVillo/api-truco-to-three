package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.events.AvailableActionsUpdatedEvent;
import com.villo.truco.domain.model.match.events.CardPlayedEvent;
import com.villo.truco.domain.model.match.events.EnvidoCalledEvent;
import com.villo.truco.domain.model.match.events.EnvidoResolvedEvent;
import com.villo.truco.domain.model.match.events.FoldedEvent;
import com.villo.truco.domain.model.match.events.HandResolvedEvent;
import com.villo.truco.domain.model.match.events.PlayerHandUpdatedEvent;
import com.villo.truco.domain.model.match.events.RoundEndedEvent;
import com.villo.truco.domain.model.match.events.RoundStartedEvent;
import com.villo.truco.domain.model.match.events.TrucoCalledEvent;
import com.villo.truco.domain.model.match.events.TrucoCancelledByEnvidoEvent;
import com.villo.truco.domain.model.match.events.TrucoRespondedEvent;
import com.villo.truco.domain.model.match.events.TurnChangedEvent;
import com.villo.truco.domain.model.match.exceptions.CannotFoldWithoutCardsException;
import com.villo.truco.domain.model.match.exceptions.InvalidRoundStateException;
import com.villo.truco.domain.model.match.exceptions.NotYourTurnException;
import com.villo.truco.domain.model.match.valueobjects.AvailableAction;
import com.villo.truco.domain.model.match.valueobjects.CurrentHandInfo;
import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.EnvidoResponse;
import com.villo.truco.domain.model.match.valueobjects.EnvidoResult;
import com.villo.truco.domain.model.match.valueobjects.PlayedHandInfo;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.model.match.valueobjects.RoundId;
import com.villo.truco.domain.model.match.valueobjects.RoundStatus;
import com.villo.truco.domain.model.match.valueobjects.ScoringResult;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.model.match.valueobjects.TrucoResponse;
import com.villo.truco.domain.shared.EntityBase;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class Round extends EntityBase<RoundId> {

  private final int roundNumber;
  private final PlayerId mano;

  private final PlayerId playerOne;
  private final PlayerId playerTwo;

  private final Hand handPlayerOne;
  private final Hand handPlayerTwo;
  private final List<PlayedHand> playedHands = new ArrayList<>();
  private final List<CardPlay> currentHandCards = new ArrayList<>();
  private final TrucoStateMachine trucoStateMachine = new TrucoStateMachine();
  private final EnvidoStateMachine envidoStateMachine = new EnvidoStateMachine();
  private RoundStatus status;
  private PlayerId currentTurn;
  private PlayerId turnBeforeTrucoCall = null;
  private PlayerId turnBeforeEnvidoCall = null;

  private Round(final RoundId id, final int roundNumber, final PlayerId mano,
      final PlayerId playerOne, final PlayerId playerTwo, final Hand handPlayerOne,
      final Hand handPlayerTwo) {

    super(id);
    this.roundNumber = roundNumber;
    this.mano = mano;
    this.playerOne = playerOne;
    this.playerTwo = playerTwo;
    this.handPlayerOne = handPlayerOne;
    this.handPlayerTwo = handPlayerTwo;
    this.currentTurn = mano;
    this.status = RoundStatus.PLAYING;
  }

  static Round create(final int roundNumber, final PlayerId mano, final PlayerId playerOne,
      final PlayerId playerTwo) {

    final var deck = Deck.create();
    final var handPlayerOne = Hand.of(deck.dealOne(), deck.dealOne(), deck.dealOne());
    final var handPlayerTwo = Hand.of(deck.dealOne(), deck.dealOne(), deck.dealOne());

    final var round = new Round(RoundId.generate(), roundNumber, mano, playerOne, playerTwo,
        handPlayerOne, handPlayerTwo);
    round.emitInitialPrivateState();
    return round;
  }

  static Round reconstruct(final RoundId id, final int roundNumber, final PlayerId mano,
      final PlayerId playerOne, final PlayerId playerTwo, final Hand handPlayerOne,
      final Hand handPlayerTwo, final List<PlayedHand> playedHands,
      final List<CardPlay> currentHandCards, final RoundStatus status, final PlayerId currentTurn,
      final PlayerId turnBeforeTrucoCall, final PlayerId turnBeforeEnvidoCall) {

    final var round = new Round(id, roundNumber, mano, playerOne, playerTwo, handPlayerOne,
        handPlayerTwo);
    round.playedHands.addAll(playedHands);
    round.currentHandCards.addAll(currentHandCards);
    round.status = status;
    round.currentTurn = currentTurn;
    round.turnBeforeTrucoCall = turnBeforeTrucoCall;
    round.turnBeforeEnvidoCall = turnBeforeEnvidoCall;
    return round;
  }

  private void emitInitialPrivateState() {

    this.addDomainEvent(new RoundStartedEvent(this.roundNumber, this.seatOf(this.mano)));
    this.addDomainEvent(
        new PlayerHandUpdatedEvent(PlayerSeat.PLAYER_ONE, this.handPlayerOne.getCards()));
    this.addDomainEvent(
        new PlayerHandUpdatedEvent(PlayerSeat.PLAYER_TWO, this.handPlayerTwo.getCards()));
    this.emitAvailableActionsUpdates();
  }

  void playCard(final PlayerId playerId, final Card card) {

    if (!RoundActionStatusSpecification.canPlayCard(this.status)) {
      throw new InvalidRoundStateException(this.status, RoundStatus.PLAYING);
    }
    this.validateTurn(playerId);

    this.getHandOf(playerId).play(card);
    this.addDomainEvent(new CardPlayedEvent(this.seatOf(playerId), card));
    this.collectHandEvents(playerId);
    this.currentHandCards.add(new CardPlay(playerId, card));

    if (this.currentHandCards.size() == 2) {
      this.resolveCurrentHand();
    } else if (AnchoDeEspadaImmediateClosurePolicy.shouldCloseRound(this.status, card,
        this.playedHands.size(), this.firstHandWasTie())) {
      this.resolveCurrentHandWithoutOpponentCard(playerId);
    } else {
      this.changeTurnTo(this.getOpponent(playerId));
    }
  }

  private void resolveCurrentHand() {

    final var cardMano = this.getCardPlayedBy(mano);
    final var cardPie = this.getCardPlayedBy(this.getOpponent(mano));

    final var winner = this.resolveHandWinner(cardMano, cardPie);
    this.finishResolvedHand(cardMano, cardPie, winner.orElse(null));
  }

  private void resolveCurrentHandWithoutOpponentCard(final PlayerId winner) {

    final var cardMano = this.getCardPlayedByOrNull(this.mano);
    final var cardPie = this.getCardPlayedByOrNull(this.getOpponent(this.mano));
    this.finishResolvedHand(cardMano, cardPie, winner);
  }

  private void finishResolvedHand(final Card cardMano, final Card cardPie,
      final PlayerId handWinner) {

    this.playedHands.add(new PlayedHand(cardMano, cardPie, handWinner));

    final var cardPlayerOne = this.getCardFor(this.playerOne, cardMano, cardPie);
    final var cardPlayerTwo = this.getCardFor(this.playerTwo, cardMano, cardPie);
    final var winnerSeat = Optional.ofNullable(handWinner).map(this::seatOf).orElse(null);
    this.addDomainEvent(new HandResolvedEvent(cardPlayerOne, cardPlayerTwo, winnerSeat));

    this.currentHandCards.clear();

    this.checkRoundFinished();

    if (this.status == RoundStatus.FINISHED) {
      this.getRoundWinner()
          .ifPresent(winnerId -> this.addDomainEvent(new RoundEndedEvent(this.seatOf(winnerId))));
      this.emitAvailableActionsUpdates();
      return;
    }

    this.changeTurnTo(handWinner != null ? handWinner : this.mano);
  }

  private Optional<PlayerId> resolveHandWinner(final Card cardMano, final Card cardPie) {

    final var valueMano = CardEvaluationService.trucoValue(cardMano);
    final var valuePie = CardEvaluationService.trucoValue(cardPie);

    if (valueMano > valuePie) {
      return Optional.of(mano);
    }
    if (valuePie > valueMano) {
      return Optional.of(this.getOpponent(mano));
    }
    return Optional.empty();
  }

  private void checkRoundFinished() {

    if (this.getRoundWinner().isPresent()) {
      this.status = RoundStatus.FINISHED;
    }
  }

  Optional<PlayerId> getRoundWinner() {

    final var handWinners = this.playedHands.stream().map(PlayedHand::winner).toList();
    return RoundTerminationPolicy.resolveWinner(handWinners, this.playerOne, this.playerTwo,
        this.mano);
  }

  void callTruco(final PlayerId playerId) {

    if (!RoundActionStatusSpecification.canCallTruco(this.status)) {
      throw new InvalidRoundStateException(this.status,
          RoundActionStatusSpecification.trucoCallAllowedStatuses());
    }
    this.validateTurn(playerId);

    final var callLevel = this.trucoStateMachine.call(playerId);

    this.turnBeforeTrucoCall = TurnRestorationPolicy.checkpointBeforeTrucoCall(this.status,
        this.currentTurn, this.turnBeforeTrucoCall);

    this.status = RoundStatus.TRUCO_IN_PROGRESS;
    this.addDomainEvent(new TrucoCalledEvent(this.seatOf(playerId), callLevel));
    this.changeTurnTo(this.getOpponent(playerId));
  }

  void acceptTruco(final PlayerId playerId) {

    if (!RoundActionStatusSpecification.canRespondTruco(this.status)) {
      throw new InvalidRoundStateException(this.status, RoundStatus.TRUCO_IN_PROGRESS);
    }
    this.validateTurn(playerId);

    final var currentCall = this.trucoStateMachine.getCurrentCall();
    this.trucoStateMachine.accept();
    this.status = RoundStatus.PLAYING;
    final var nextTurn = TurnRestorationPolicy.turnAfterTrucoAccepted(this.turnBeforeTrucoCall);
    this.turnBeforeTrucoCall = null;
    this.addDomainEvent(
        new TrucoRespondedEvent(this.seatOf(playerId), TrucoResponse.QUIERO, currentCall));
    this.changeTurnTo(nextTurn);
  }

  ScoringResult rejectTruco(final PlayerId playerId) {

    if (!RoundActionStatusSpecification.canRespondTruco(this.status)) {
      throw new InvalidRoundStateException(this.status, RoundStatus.TRUCO_IN_PROGRESS);
    }
    this.validateTurn(playerId);

    final var currentCall = this.trucoStateMachine.getCurrentCall();
    final var points = this.trucoStateMachine.pointsIfRejected();
    final var winner = this.getOpponent(playerId);
    this.status = RoundStatus.FINISHED;
    this.addDomainEvent(
        new TrucoRespondedEvent(this.seatOf(playerId), TrucoResponse.NO_QUIERO, currentCall));
    this.addDomainEvent(new RoundEndedEvent(this.seatOf(winner)));
    this.emitAvailableActionsUpdates();
    return new ScoringResult(winner, points);
  }

  ScoringResult acceptTrucoAndFold(final PlayerId playerId) {

    if (!RoundActionStatusSpecification.canRespondTruco(this.status)) {
      throw new InvalidRoundStateException(this.status, RoundStatus.TRUCO_IN_PROGRESS);
    }
    this.validateTurn(playerId);
    this.validateHasCards(playerId);

    final var currentCall = this.trucoStateMachine.getCurrentCall();
    final var points = this.trucoStateMachine.pointsIfAccepted();
    final var winner = this.getOpponent(playerId);
    this.status = RoundStatus.FINISHED;
    this.addDomainEvent(
        new TrucoRespondedEvent(this.seatOf(playerId), TrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO,
            currentCall));
    this.addDomainEvent(new RoundEndedEvent(this.seatOf(winner)));
    this.emitAvailableActionsUpdates();
    return new ScoringResult(winner, points);
  }

  ScoringResult fold(final PlayerId playerId) {

    this.validateTurn(playerId);

    if (!RoundActionStatusSpecification.canFold(this.status)) {
      throw new InvalidRoundStateException(this.status, RoundStatus.PLAYING);
    }

    this.validateFoldAllowed(playerId);

    this.status = RoundStatus.FINISHED;
    final var winner = this.getOpponent(playerId);
    this.addDomainEvent(new FoldedEvent(this.seatOf(playerId)));
    this.addDomainEvent(new RoundEndedEvent(this.seatOf(winner)));
    this.emitAvailableActionsUpdates();

    if (this.trucoStateMachine.hasBeenCalled()) {
      return new ScoringResult(winner, this.trucoStateMachine.getPointsAtStake());
    }

    // Regla anterior comentada: en primera mano, si mano se iba al mazo sin envido resuelto,
    // se otorgaban 2 puntos al rival. En partidas a 3 puntos exactos esta regla habilitaba
    // un loop de folds que rompía la partida. Ahora ese fold está bloqueado en
    // validateFoldAllowed(...), salvo que el truco ya haya sido cantado y aceptado.
    // if (isMano && this.isFirstHand() && !this.envidoFlow.isResolved()) {
    //   return new ScoringResult(winner, 2);
    // }

    return new ScoringResult(winner, 1);
  }

  private void validateHasCards(final PlayerId playerId) {

    if (this.getHandOf(playerId).getCards().isEmpty()) {
      throw new CannotFoldWithoutCardsException();
    }
  }

  private void validateFoldAllowed(final PlayerId playerId) {

    FoldPolicy.validateFoldAllowed(playerId.equals(this.mano), this.isFirstHand(),
        this.envidoStateMachine.isResolved(), this.trucoStateMachine.hasBeenCalled());
  }

  void callEnvido(final PlayerId playerId, final EnvidoCall call) {

    final var previousStatus = this.status;
    this.validateTurn(playerId);
    this.validateCanCallEnvido(playerId);

    this.turnBeforeEnvidoCall = TurnRestorationPolicy.checkpointBeforeEnvidoCall(this.status,
        this.currentTurn, this.turnBeforeTrucoCall, this.turnBeforeEnvidoCall);

    this.envidoStateMachine.call(call);
    this.status = RoundStatus.ENVIDO_IN_PROGRESS;
    this.addDomainEvent(new EnvidoCalledEvent(this.seatOf(playerId), call));

    if (TurnRestorationPolicy.shouldCancelTrucoOnEnvido(previousStatus,
        this.trucoStateMachine.getCurrentCall())) {
      this.trucoStateMachine.cancel();
      this.turnBeforeTrucoCall = null;
      this.addDomainEvent(new TrucoCancelledByEnvidoEvent());
    }

    this.changeTurnTo(this.getOpponent(playerId));
  }

  EnvidoResult acceptEnvido(final PlayerId playerId, final int scorePlayerOne,
      final int scorePlayerTwo) {

    if (!RoundActionStatusSpecification.canRespondEnvido(this.status)) {
      throw new InvalidRoundStateException(this.status, RoundStatus.ENVIDO_IN_PROGRESS);
    }
    this.validateTurn(playerId);

    final var pointsMano = CardEvaluationService.envidoScore(this.getHandOf(this.mano).getCards());
    final var pointsPie = CardEvaluationService.envidoScore(
        this.getHandOf(this.getOpponent(this.mano)).getCards());

    final var winner = pointsMano >= pointsPie ? this.mano : this.getOpponent(this.mano);
    final var pointsWon = this.envidoStateMachine.calculateAcceptedPoints(scorePlayerOne,
        scorePlayerTwo, winner, this.playerOne);

    this.envidoStateMachine.resolve();
    this.status = RoundStatus.PLAYING;
    final var winnerSeat = this.seatOf(winner);
    final var nextTurn = TurnRestorationPolicy.turnAfterEnvidoResolved(this.turnBeforeEnvidoCall);
    this.turnBeforeEnvidoCall = null;
    final boolean manoWon = pointsMano >= pointsPie;
    this.addDomainEvent(new EnvidoResolvedEvent(EnvidoResponse.QUIERO, winnerSeat, pointsMano,
        manoWon ? null : pointsPie));
    this.changeTurnTo(nextTurn);

    return new EnvidoResult(pointsMano, pointsPie, winner, pointsWon);
  }

  ScoringResult rejectEnvido(final PlayerId playerId) {

    if (!RoundActionStatusSpecification.canRespondEnvido(this.status)) {
      throw new InvalidRoundStateException(this.status, RoundStatus.ENVIDO_IN_PROGRESS);
    }
    this.validateTurn(playerId);

    final var winner = this.getOpponent(playerId);

    final var points = this.envidoStateMachine.calculateRejectedPoints();
    this.envidoStateMachine.resolve();
    this.status = RoundStatus.PLAYING;
    this.addDomainEvent(
        new EnvidoResolvedEvent(EnvidoResponse.NO_QUIERO, this.seatOf(winner), null, null));
    final var nextTurn = TurnRestorationPolicy.turnAfterEnvidoResolved(this.turnBeforeEnvidoCall);
    this.turnBeforeEnvidoCall = null;
    this.changeTurnTo(nextTurn);

    return new ScoringResult(winner, points);
  }

  private void validateCanCallEnvido(final PlayerId playerId) {

    EnvidoCallPolicy.validateCanCallEnvido(this.status, this.isFirstHand(),
        this.hasPlayerPlayedInCurrentHand(playerId), this.envidoStateMachine.isResolved(),
        this.trucoStateMachine.hasBeenCalled(), this.trucoStateMachine.getCurrentCall());
  }

  TrucoCall getCurrentTrucoCall() {

    return this.trucoStateMachine.getCurrentCall();
  }

  boolean isFirstHand() {

    return playedHands.isEmpty();
  }

  private boolean firstHandWasTie() {

    return this.playedHands.size() == 1 && this.playedHands.getFirst().winner() == null;
  }

  boolean hasPlayerPlayedInCurrentHand(final PlayerId playerId) {

    return this.currentHandCards.stream().anyMatch(cp -> cp.playerId().equals(playerId));
  }

  List<AvailableAction> getAvailableActions(final PlayerId playerId) {

    return AvailableActionsPolicy.resolve(this.status, playerId, this.currentTurn,
        this.trucoStateMachine, this.envidoStateMachine, this.isFirstHand(),
        this.hasPlayerPlayedInCurrentHand(playerId), this.mano.equals(playerId),
        !this.getHandOf(playerId).getCards().isEmpty());
  }

  List<PlayedHandInfo> getPlayedHands() {

    return this.playedHands.stream().map(
        ph -> new PlayedHandInfo(this.getCardFor(this.playerOne, ph.cardMano(), ph.cardPie()),
            this.getCardFor(this.playerTwo, ph.cardMano(), ph.cardPie()), ph.winner())).toList();
  }

  private Card getCardFor(final PlayerId player, final Card cardMano, final Card cardPie) {

    return player.equals(this.mano) ? cardMano : cardPie;
  }

  CurrentHandInfo getCurrentHandInfo() {

    final var cardPlayerOne = this.currentHandCards.stream()
        .filter(cp -> cp.playerId().equals(this.playerOne)).map(CardPlay::card).findFirst()
        .orElse(null);

    final var cardPlayerTwo = this.currentHandCards.stream()
        .filter(cp -> cp.playerId().equals(this.playerTwo)).map(CardPlay::card).findFirst()
        .orElse(null);

    return new CurrentHandInfo(cardPlayerOne, cardPlayerTwo, this.mano);
  }

  PlayerId getManoPlayer() {

    return this.mano;
  }

  int getTrucoPointsAtStake() {

    return this.trucoStateMachine.getPointsAtStake();
  }

  RoundStatus getStatus() {

    return status;
  }

  PlayerId getCurrentTurn() {

    return currentTurn;
  }

  Hand getHandOf(final PlayerId playerId) {

    return playerId.equals(playerOne) ? handPlayerOne : handPlayerTwo;
  }

  private Card getCardPlayedBy(final PlayerId playerId) {

    return this.currentHandCards.stream().filter(cp -> cp.playerId().equals(playerId))
        .map(CardPlay::card).findFirst().orElseThrow();
  }

  private Card getCardPlayedByOrNull(final PlayerId playerId) {

    return this.currentHandCards.stream().filter(cp -> cp.playerId().equals(playerId))
        .map(CardPlay::card).findFirst().orElse(null);
  }

  private PlayerId getOpponent(final PlayerId playerId) {

    return playerId.equals(playerOne) ? playerTwo : playerOne;
  }

  private PlayerSeat seatOf(final PlayerId playerId) {

    return playerId.equals(playerOne) ? PlayerSeat.PLAYER_ONE : PlayerSeat.PLAYER_TWO;
  }

  private void emitTurnChanged() {

    this.addDomainEvent(new TurnChangedEvent(this.seatOf(this.currentTurn)));
  }

  private void changeTurnTo(final PlayerId nextTurn) {

    this.currentTurn = nextTurn;
    this.emitTurnChanged();
    this.emitAvailableActionsUpdates();
  }

  private void collectHandEvents(final PlayerId playerId) {

    final var hand = this.getHandOf(playerId);
    final var seat = this.seatOf(playerId);
    final var mappedEvents = HandDomainEventMapper.toPlayerHandUpdatedEvents(hand.getDomainEvents(),
        seat);
    hand.clearDomainEvents();
    mappedEvents.forEach(this::addDomainEvent);
  }

  private void emitAvailableActionsUpdates() {

    this.addDomainEvent(new AvailableActionsUpdatedEvent(PlayerSeat.PLAYER_ONE,
        this.getAvailableActions(this.playerOne)));
    this.addDomainEvent(new AvailableActionsUpdatedEvent(PlayerSeat.PLAYER_TWO,
        this.getAvailableActions(this.playerTwo)));
  }

  private void validateTurn(final PlayerId playerId) {

    if (!this.currentTurn.equals(playerId)) {
      throw new NotYourTurnException(playerId);
    }
  }

  int getRoundNumber() {

    return this.roundNumber;
  }

  PlayerId getMano() {

    return this.mano;
  }

  PlayerId getPlayerOne() {

    return this.playerOne;
  }

  PlayerId getPlayerTwo() {

    return this.playerTwo;
  }

  Hand getHandPlayerOne() {

    return this.handPlayerOne;
  }

  Hand getHandPlayerTwo() {

    return this.handPlayerTwo;
  }

  List<PlayedHand> getPlayedHandsInternal() {

    return this.playedHands;
  }

  List<CardPlay> getCurrentHandCards() {

    return this.currentHandCards;
  }

  TrucoStateMachine getTrucoStateMachine() {

    return this.trucoStateMachine;
  }

  EnvidoStateMachine getEnvidoStateMachine() {

    return this.envidoStateMachine;
  }

  PlayerId getTurnBeforeTrucoCall() {

    return this.turnBeforeTrucoCall;
  }

  PlayerId getTurnBeforeEnvidoCall() {

    return this.turnBeforeEnvidoCall;
  }

  record CardPlay(PlayerId playerId, Card card) {

  }

  record PlayedHand(Card cardMano, Card cardPie, PlayerId winner) {

  }

}
