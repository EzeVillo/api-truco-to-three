package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.events.AvailableActionsUpdatedEvent;
import com.villo.truco.domain.model.match.events.CardPlayedEvent;
import com.villo.truco.domain.model.match.events.EnvidoCalledEvent;
import com.villo.truco.domain.model.match.events.EnvidoResolvedEvent;
import com.villo.truco.domain.model.match.events.FoldedEvent;
import com.villo.truco.domain.model.match.events.HandChangedEvent;
import com.villo.truco.domain.model.match.events.HandResolvedEvent;
import com.villo.truco.domain.model.match.events.PlayerHandUpdatedEvent;
import com.villo.truco.domain.model.match.events.RoundEndedEvent;
import com.villo.truco.domain.model.match.events.RoundStartedEvent;
import com.villo.truco.domain.model.match.events.TrucoCalledEvent;
import com.villo.truco.domain.model.match.events.TrucoRespondedEvent;
import com.villo.truco.domain.model.match.events.TurnChangedEvent;
import com.villo.truco.domain.model.match.exceptions.EnvidoNotAllowedException;
import com.villo.truco.domain.model.match.exceptions.InvalidRoundStateException;
import com.villo.truco.domain.model.match.exceptions.InvalidTrucoCallException;
import com.villo.truco.domain.model.match.exceptions.NotYourTurnException;
import com.villo.truco.domain.model.match.valueobjects.ActionType;
import com.villo.truco.domain.model.match.valueobjects.AvailableAction;
import com.villo.truco.domain.model.match.valueobjects.Card;
import com.villo.truco.domain.model.match.valueobjects.CurrentHandInfo;
import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.EnvidoResponse;
import com.villo.truco.domain.model.match.valueobjects.EnvidoResult;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.match.valueobjects.PlayedHandInfo;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.model.match.valueobjects.RoundId;
import com.villo.truco.domain.model.match.valueobjects.RoundStatus;
import com.villo.truco.domain.model.match.valueobjects.ScoringResult;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.model.match.valueobjects.TrucoResponse;
import com.villo.truco.domain.shared.EntityBase;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class Round extends EntityBase<RoundId> {

  private final int roundNumber;
  private final PlayerId mano;

  private final PlayerId playerOne;
  private final PlayerId playerTwo;
  private final MatchRules rules;

  private final Hand handPlayerOne;
  private final Hand handPlayerTwo;
  private final List<PlayedHand> playedHands = new ArrayList<>();
  private final List<CardPlay> currentHandCards = new ArrayList<>();
  private final List<EnvidoCall> envidoChain = new ArrayList<>();
  private RoundStatus status;
  private PlayerId currentTurn;
  private PlayerId trucoCaller = null;
  private PlayerId turnBeforeTrucoCall = null;
  private TrucoCall currentTrucoCall = null;
  private int trucoPointsAtStake = 1;
  private PlayerId turnBeforeEnvidoCall = null;
  private boolean envidoResolved = false;

  private Round(final RoundId id, final int roundNumber, final PlayerId mano,
      final PlayerId playerOne, final PlayerId playerTwo, final Hand handPlayerOne,
      final Hand handPlayerTwo, final MatchRules rules) {

    super(id);
    this.roundNumber = roundNumber;
    this.mano = mano;
    this.playerOne = playerOne;
    this.playerTwo = playerTwo;
    this.rules = rules;
    this.handPlayerOne = handPlayerOne;
    this.handPlayerTwo = handPlayerTwo;
    this.currentTurn = mano;
    this.status = RoundStatus.PLAYING;
  }

  static Round create(final int roundNumber, final PlayerId mano, final PlayerId playerOne,
      final PlayerId playerTwo, final MatchRules rules) {

    Objects.requireNonNull(rules, "MatchRules cannot be null");

    final var deck = Deck.create();
    final var handPlayerOne = Hand.of(deck.dealOne(), deck.dealOne(), deck.dealOne());
    final var handPlayerTwo = Hand.of(deck.dealOne(), deck.dealOne(), deck.dealOne());

    final var round = new Round(RoundId.generate(), roundNumber, mano, playerOne, playerTwo,
        handPlayerOne, handPlayerTwo, rules);
    round.emitInitialPrivateState();
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

    this.validateStatus(RoundStatus.PLAYING);
    this.validateTurn(playerId);

    this.getHandOf(playerId).play(card);
    this.addDomainEvent(new CardPlayedEvent(this.seatOf(playerId), card));
    this.collectHandEvents(playerId);
    this.currentHandCards.add(new CardPlay(playerId, card));

    if (this.currentHandCards.size() == 2) {
      this.resolveCurrentHand();
      if (this.status == RoundStatus.FINISHED) {
        this.emitAvailableActionsUpdates();
      }
    } else {
      this.changeTurnTo(this.getOpponent(playerId));
    }
  }

  private void resolveCurrentHand() {

    final var cardMano = this.getCardPlayedBy(mano);
    final var cardPie = this.getCardPlayedBy(this.getOpponent(mano));

    final var winner = this.resolveHandWinner(cardMano, cardPie);
    this.playedHands.add(new PlayedHand(cardMano, cardPie, winner.orElse(null)));

    final var cardPlayerOne = this.getCardFor(this.playerOne, cardMano, cardPie);
    final var cardPlayerTwo = this.getCardFor(this.playerTwo, cardMano, cardPie);
    final var winnerSeat = winner.map(this::seatOf).orElse(null);
    this.addDomainEvent(new HandResolvedEvent(cardPlayerOne, cardPlayerTwo, winnerSeat));

    this.currentHandCards.clear();

    this.checkRoundFinished();

    if (this.status == RoundStatus.FINISHED) {
      this.getRoundWinner()
          .ifPresent(winnerId -> this.addDomainEvent(new RoundEndedEvent(this.seatOf(winnerId))));
    } else {
      this.changeTurnTo(winner.orElse(mano));
    }
  }

  private Optional<PlayerId> resolveHandWinner(final Card cardMano, final Card cardPie) {

    final var valueMano = TrucoCardValue.of(cardMano);
    final var valuePie = TrucoCardValue.of(cardPie);

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

    final int winsPlayerOne = this.countWins(playerOne);
    final int winsPlayerTwo = this.countWins(playerTwo);

    if (winsPlayerOne >= 2) {
      return Optional.of(playerOne);
    }
    if (winsPlayerTwo >= 2) {
      return Optional.of(playerTwo);
    }

    if (this.playedHands.size() == 3) {
      return Optional.of(mano);
    }

    if (this.playedHands.size() == 2) {
      final var firstHandWinner = playedHands.get(0).winner();
      final var secondHandWinner = playedHands.get(1).winner();

      if (firstHandWinner == null && secondHandWinner != null) {
        return Optional.of(secondHandWinner);
      }
    }

    return Optional.empty();
  }

  void callTruco(final PlayerId playerId) {

    if (this.status != RoundStatus.PLAYING && this.status != RoundStatus.TRUCO_IN_PROGRESS) {
      throw new InvalidRoundStateException(this.status,
          List.of(RoundStatus.PLAYING, RoundStatus.TRUCO_IN_PROGRESS));
    }
    this.validateTurn(playerId);

    if (this.currentTrucoCall == null) {
      this.currentTrucoCall = TrucoCall.TRUCO;
    } else if (this.currentTrucoCall.hasNext() && !playerId.equals(trucoCaller)) {
      this.currentTrucoCall = this.currentTrucoCall.next();
    } else {
      throw new InvalidTrucoCallException();
    }

    if (this.status != RoundStatus.TRUCO_IN_PROGRESS) {
      this.turnBeforeTrucoCall = this.currentTurn;
    }

    this.trucoCaller = playerId;
    this.status = RoundStatus.TRUCO_IN_PROGRESS;
    this.addDomainEvent(new TrucoCalledEvent(this.seatOf(playerId), this.currentTrucoCall));
    this.changeTurnTo(this.getOpponent(playerId));
  }

  void acceptTruco(final PlayerId playerId) {

    this.validateStatus(RoundStatus.TRUCO_IN_PROGRESS);
    this.validateTurn(playerId);

    final var currentCall = this.currentTrucoCall;
    this.trucoPointsAtStake = this.currentTrucoCall.pointsIfAccepted();
    this.status = RoundStatus.PLAYING;
    final var nextTurn = this.turnBeforeTrucoCall;
    this.turnBeforeTrucoCall = null;
    this.addDomainEvent(
        new TrucoRespondedEvent(this.seatOf(playerId), TrucoResponse.QUIERO, currentCall));
    this.changeTurnTo(nextTurn);
  }

  ScoringResult rejectTruco(final PlayerId playerId) {

    this.validateStatus(RoundStatus.TRUCO_IN_PROGRESS);
    this.validateTurn(playerId);

    final var currentCall = this.currentTrucoCall;
    final var winner = this.getOpponent(playerId);
    this.status = RoundStatus.FINISHED;
    this.addDomainEvent(
        new TrucoRespondedEvent(this.seatOf(playerId), TrucoResponse.NO_QUIERO, currentCall));
    this.addDomainEvent(new RoundEndedEvent(this.seatOf(winner)));
    this.emitAvailableActionsUpdates();
    return new ScoringResult(winner, this.currentTrucoCall.pointsIfRejected());
  }

  ScoringResult acceptTrucoAndFold(final PlayerId playerId) {

    this.validateStatus(RoundStatus.TRUCO_IN_PROGRESS);
    this.validateTurn(playerId);

    final var currentCall = this.currentTrucoCall;
    final var winner = this.getOpponent(playerId);
    this.status = RoundStatus.FINISHED;
    this.addDomainEvent(
        new TrucoRespondedEvent(this.seatOf(playerId), TrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO,
            currentCall));
    this.addDomainEvent(new RoundEndedEvent(this.seatOf(winner)));
    this.emitAvailableActionsUpdates();
    return new ScoringResult(winner, this.currentTrucoCall.pointsIfAccepted());
  }

  ScoringResult fold(final PlayerId playerId) {

    this.validateTurn(playerId);

    if (this.status != RoundStatus.PLAYING) {
      throw new InvalidRoundStateException(this.status, RoundStatus.PLAYING);
    }

    this.status = RoundStatus.FINISHED;
    final var winner = this.getOpponent(playerId);
    this.addDomainEvent(new FoldedEvent(this.seatOf(playerId)));
    this.addDomainEvent(new RoundEndedEvent(this.seatOf(winner)));
    this.emitAvailableActionsUpdates();

    if (this.currentTrucoCall != null) {
      return new ScoringResult(winner, this.currentTrucoCall.pointsIfAccepted());
    }

    final var isMano = playerId.equals(this.mano);

    if (isMano && this.isFirstHand() && !this.envidoResolved) {
      return new ScoringResult(winner, 2);
    }

    return new ScoringResult(winner, 1);
  }

  void callEnvido(final PlayerId playerId, final EnvidoCall call) {

    this.validateTurn(playerId);
    this.validateCanCallEnvido(playerId, call);

    if (this.status != RoundStatus.ENVIDO_IN_PROGRESS) {
      this.turnBeforeEnvidoCall = this.currentTurn;
    }

    this.envidoChain.add(call);
    this.status = RoundStatus.ENVIDO_IN_PROGRESS;
    this.addDomainEvent(new EnvidoCalledEvent(this.seatOf(playerId), call));

    if (this.currentTrucoCall != null) {
      this.currentTrucoCall = null;
      this.trucoCaller = null;
      this.turnBeforeTrucoCall = null;
    }

    this.changeTurnTo(this.getOpponent(playerId));
  }

  EnvidoResult acceptEnvido(final PlayerId playerId, final int scorePlayerOne,
      final int scorePlayerTwo) {

    this.validateStatus(RoundStatus.ENVIDO_IN_PROGRESS);
    this.validateTurn(playerId);

    final var pointsMano = EnvidoCalculator.calculate(this.getHandOf(this.mano).getCards());
    final var pointsPie = EnvidoCalculator.calculate(
        this.getHandOf(this.getOpponent(this.mano)).getCards());

    final var winner = pointsMano >= pointsPie ? this.mano : this.getOpponent(this.mano);
    final var pointsWon = this.calculateEnvidoChainPoints(scorePlayerOne, scorePlayerTwo, winner);

    this.envidoResolved = true;
    this.status = RoundStatus.PLAYING;
    final var winnerSeat = this.seatOf(winner);
    final var nextTurn = this.turnBeforeEnvidoCall;
    this.turnBeforeEnvidoCall = null;
    final boolean manoWon = pointsMano >= pointsPie;
    this.addDomainEvent(new EnvidoResolvedEvent(EnvidoResponse.QUIERO, winnerSeat, pointsMano,
        manoWon ? null : pointsPie));
    this.changeTurnTo(nextTurn);

    return new EnvidoResult(pointsMano, pointsPie, winner, pointsWon);
  }

  ScoringResult rejectEnvido(final PlayerId playerId) {

    this.validateStatus(RoundStatus.ENVIDO_IN_PROGRESS);
    this.validateTurn(playerId);

    final var winner = this.getOpponent(playerId);

    final var points = this.calculateRejectedEnvidoPoints();
    this.envidoResolved = true;
    this.status = RoundStatus.PLAYING;
    this.addDomainEvent(
        new EnvidoResolvedEvent(EnvidoResponse.NO_QUIERO, this.seatOf(winner), null, null));
    final var nextTurn = this.turnBeforeEnvidoCall;
    this.turnBeforeEnvidoCall = null;
    this.changeTurnTo(nextTurn);

    return new ScoringResult(winner, points);
  }

  private void validateCanCallEnvido(final PlayerId playerId, final EnvidoCall call) {

    if (!this.isFirstHand()) {
      throw new EnvidoNotAllowedException("El envido solo se puede cantar en la primera mano");
    }

    if (this.hasPlayerPlayedInCurrentHand(playerId)
        && this.status != RoundStatus.ENVIDO_IN_PROGRESS) {
      throw new EnvidoNotAllowedException("No podes cantar envido si ya jugaste una carta");
    }

    if (this.envidoResolved) {
      throw new EnvidoNotAllowedException("El envido ya fue resuelto en esta ronda");
    }

    if (this.status == RoundStatus.ENVIDO_IN_PROGRESS) {
      final var lastCall = envidoChain.getLast();
      if (!this.canRaiseEnvidoWith(call)) {
        throw new EnvidoNotAllowedException("No se puede responder " + lastCall + " con " + call);
      }

      final boolean alreadyHasTwoEnvidos =
          this.envidoChain.stream().filter(c -> c == EnvidoCall.ENVIDO).count() >= 2;

      if (call == EnvidoCall.ENVIDO && alreadyHasTwoEnvidos) {
        throw new EnvidoNotAllowedException("No se puede cantar más de dos envidos");
      }

    } else if (status != RoundStatus.PLAYING && status != RoundStatus.TRUCO_IN_PROGRESS) {
      throw new EnvidoNotAllowedException("No se puede cantar envido en este momento");
    }
  }

  private boolean canRaiseEnvidoWith(final EnvidoCall call) {

    if (this.envidoChain.isEmpty()) {
      return true;
    }

    final var lastCall = this.envidoChain.getLast();
    if (!lastCall.canBeRaisedWith(call)) {
      return false;
    }

    if (call == EnvidoCall.ENVIDO) {
      final long envidoCount = this.envidoChain.stream().filter(c -> c == EnvidoCall.ENVIDO)
          .count();
      return envidoCount < 2;
    }
    return true;
  }

  private int calculateEnvidoChainPoints(final int scorePlayerOne, final int scorePlayerTwo,
      final PlayerId winner) {

    final var hasFaltaEnvido = this.envidoChain.contains(EnvidoCall.FALTA_ENVIDO);

    if (hasFaltaEnvido) {
      final var rivalScore = winner.equals(this.playerOne) ? scorePlayerTwo : scorePlayerOne;
      return this.rules.pointsToWinGame() - rivalScore;
    }

    return this.envidoChain.stream().mapToInt(EnvidoCall::points).sum();
  }

  private int calculateRejectedEnvidoPoints() {

    if (this.envidoChain.size() == 1) {
      return 1;
    }

    final var chainWithoutLast = this.envidoChain.subList(0, this.envidoChain.size() - 1);
    return chainWithoutLast.stream().filter(call -> call != EnvidoCall.FALTA_ENVIDO)
        .mapToInt(EnvidoCall::points).sum();
  }

  TrucoCall getCurrentTrucoCall() {

    return currentTrucoCall;
  }

  boolean isFirstHand() {

    return playedHands.isEmpty();
  }

  boolean hasPlayerPlayedInCurrentHand(final PlayerId playerId) {

    return this.currentHandCards.stream().anyMatch(cp -> cp.playerId().equals(playerId));
  }

  List<AvailableAction> getAvailableActions(final PlayerId playerId) {

    if (this.status == RoundStatus.FINISHED) {
      return List.of();
    }

    if (!this.currentTurn.equals(playerId)) {
      return List.of();
    }

    final var actions = new ArrayList<AvailableAction>();

    if (this.status == RoundStatus.PLAYING) {
      actions.add(AvailableAction.of(ActionType.PLAY_CARD));
      actions.add(AvailableAction.of(ActionType.FOLD));
      this.addTrucoActions(playerId, actions);
      this.addEnvidoActions(playerId, actions);
    }

    if (this.status == RoundStatus.TRUCO_IN_PROGRESS) {
      actions.add(AvailableAction.of(ActionType.RESPOND_TRUCO, TrucoResponse.QUIERO.name()));
      actions.add(AvailableAction.of(ActionType.RESPOND_TRUCO, TrucoResponse.NO_QUIERO.name()));
      actions.add(AvailableAction.of(ActionType.RESPOND_TRUCO,
          TrucoResponse.QUIERO_Y_ME_VOY_AL_MAZO.name()));

      if (this.currentTrucoCall.hasNext()) {
        actions.add(AvailableAction.of(ActionType.CALL_TRUCO, this.currentTrucoCall.next().name()));
      }
    }

    if (this.status == RoundStatus.ENVIDO_IN_PROGRESS) {
      actions.add(AvailableAction.of(ActionType.RESPOND_ENVIDO, EnvidoResponse.QUIERO.name()));
      actions.add(AvailableAction.of(ActionType.RESPOND_ENVIDO, EnvidoResponse.NO_QUIERO.name()));
      this.addEnvidoRaiseActions(actions);
    }

    return actions;
  }

  private void addTrucoActions(final PlayerId playerId, final List<AvailableAction> actions) {

    if (this.currentTrucoCall == null) {
      actions.add(AvailableAction.of(ActionType.CALL_TRUCO, TrucoCall.TRUCO.name()));
    } else if (this.currentTrucoCall.hasNext() && !playerId.equals(this.trucoCaller)) {
      actions.add(AvailableAction.of(ActionType.CALL_TRUCO, this.currentTrucoCall.next().name()));
    }
  }

  private void addEnvidoActions(final PlayerId playerId, final List<AvailableAction> actions) {

    if (!this.isFirstHand() || this.envidoResolved) {
      return;
    }
    if (this.hasPlayerPlayedInCurrentHand(playerId)) {
      return;
    }

    if (this.envidoChain.isEmpty()) {
      actions.add(AvailableAction.of(ActionType.CALL_ENVIDO, EnvidoCall.ENVIDO.name()));
      actions.add(AvailableAction.of(ActionType.CALL_ENVIDO, EnvidoCall.REAL_ENVIDO.name()));
      actions.add(AvailableAction.of(ActionType.CALL_ENVIDO, EnvidoCall.FALTA_ENVIDO.name()));
    }
  }

  private void addEnvidoRaiseActions(final List<AvailableAction> actions) {

    for (final var call : EnvidoCall.values()) {
      if (this.canRaiseEnvidoWith(call)) {
        actions.add(AvailableAction.of(ActionType.CALL_ENVIDO, call.name()));
      }
    }
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

    return trucoPointsAtStake;
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

  private int countWins(final PlayerId playerId) {

    return (int) playedHands.stream().filter(ph -> playerId.equals(ph.winner())).count();
  }

  private Card getCardPlayedBy(final PlayerId playerId) {

    return this.currentHandCards.stream().filter(cp -> cp.playerId().equals(playerId))
        .map(CardPlay::card).findFirst().orElseThrow();
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
    for (final var event : hand.getDomainEvents()) {
      if (event instanceof HandChangedEvent changed) {
        this.addDomainEvent(new PlayerHandUpdatedEvent(seat, changed.getRemainingCards()));
      }
    }
    hand.clearDomainEvents();
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

  private void validateStatus(final RoundStatus expected) {

    if (this.status != expected) {
      throw new InvalidRoundStateException(this.status, expected);
    }
  }

  private record CardPlay(PlayerId playerId, Card card) {

  }

  private record PlayedHand(Card cardMano, Card cardPie, PlayerId winner) {

  }

}
