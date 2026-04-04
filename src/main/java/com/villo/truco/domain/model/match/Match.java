package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.events.GameScoreChangedEvent;
import com.villo.truco.domain.model.match.events.GameStartedEvent;
import com.villo.truco.domain.model.match.events.MatchAbandonedEvent;
import com.villo.truco.domain.model.match.events.MatchCancelledEvent;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.MatchEventEnvelope;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.events.MatchPlayerLeftEvent;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import com.villo.truco.domain.model.match.events.PlayerReadyEvent;
import com.villo.truco.domain.model.match.events.PublicMatchLobbyOpenedEvent;
import com.villo.truco.domain.model.match.events.ScoreChangedEvent;
import com.villo.truco.domain.model.match.exceptions.InvalidInviteCodeException;
import com.villo.truco.domain.model.match.exceptions.InvalidMatchStateException;
import com.villo.truco.domain.model.match.exceptions.MatchNotFullException;
import com.villo.truco.domain.model.match.exceptions.PlayerNotInMatchException;
import com.villo.truco.domain.model.match.exceptions.PrivateMatchVisibilityAccessException;
import com.villo.truco.domain.model.match.exceptions.PublicMatchLobbyUnavailableException;
import com.villo.truco.domain.model.match.exceptions.SamePlayerMatchException;
import com.villo.truco.domain.model.match.valueobjects.AvailableAction;
import com.villo.truco.domain.model.match.valueobjects.CurrentHandInfo;
import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.model.match.valueobjects.PlayedHandInfo;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.model.match.valueobjects.RoundStatus;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.shared.AggregateBase;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Match extends AggregateBase<MatchId> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Match.class);

  private final MatchRules rules;

  private final PlayerId playerOne;
  private final Visibility visibility;
  private final InviteCode inviteCode;
  private PlayerId playerTwo;
  private PlayerId firstManoOfGame = null;

  private MatchStatus status;

  private int gamesWonPlayerOne;
  private int gamesWonPlayerTwo;
  private int gameNumber;

  private int scorePlayerOne;
  private int scorePlayerTwo;

  private Round currentRound;
  private int roundNumber;

  private boolean readyPlayerOne;
  private boolean readyPlayerTwo;

  private Match(final MatchId id, final PlayerId playerOne, final PlayerId playerTwo,
      final InviteCode inviteCode, final MatchRules rules, final Visibility visibility,
      final MatchStatus initialStatus) {

    super(id);
    this.playerOne = playerOne;
    this.playerTwo = playerTwo;
    this.inviteCode = inviteCode;
    this.rules = rules;
    this.visibility = visibility;
    this.status = initialStatus;
    this.gamesWonPlayerOne = 0;
    this.gamesWonPlayerTwo = 0;
    this.gameNumber = 0;
    this.readyPlayerOne = false;
    this.readyPlayerTwo = false;
  }

  public static Match create(final PlayerId playerOne, final MatchRules rules,
      final Visibility visibility) {

    Objects.requireNonNull(playerOne, "PlayerOne cannot be null");
    Objects.requireNonNull(rules, "MatchRules cannot be null");
    Objects.requireNonNull(visibility, "Visibility cannot be null");
    final var inviteCode = visibility == Visibility.PRIVATE ? InviteCode.generate() : null;
    final var match = new Match(MatchId.generate(), playerOne, null, inviteCode, rules, visibility,
        MatchStatus.WAITING_FOR_PLAYERS);
    if (visibility == Visibility.PUBLIC) {
      match.addDomainEvent(new PublicMatchLobbyOpenedEvent(match.getId(), playerOne));
    }
    LOGGER.info("Match created: matchId={}, playerOne={}, visibility={}, rules={gamesToWin={}}",
        match.getId(), playerOne, visibility, rules.gamesToWin());
    return match;
  }

  public static Match createReady(final PlayerId playerOne, final PlayerId playerTwo,
      final MatchRules rules) {

    Objects.requireNonNull(playerOne, "PlayerOne cannot be null");
    Objects.requireNonNull(playerTwo, "PlayerTwo cannot be null");
    Objects.requireNonNull(rules, "MatchRules cannot be null");
    if (playerOne.equals(playerTwo)) {
      throw new SamePlayerMatchException();
    }
    final var match = new Match(MatchId.generate(), playerOne, playerTwo, InviteCode.generate(),
        rules, Visibility.PRIVATE, MatchStatus.READY);
    LOGGER.info("Ready match created: matchId={}, playerOne={}, playerTwo={}", match.getId(),
        playerOne, playerTwo);
    return match;
  }

  static Match reconstruct(final MatchId id, final PlayerId playerOne, final PlayerId playerTwo,
      final InviteCode inviteCode, final MatchRules rules, final Visibility visibility,
      final MatchStatus status, final int gamesWonPlayerOne, final int gamesWonPlayerTwo,
      final int gameNumber, final int scorePlayerOne, final int scorePlayerTwo,
      final int roundNumber, final boolean readyPlayerOne, final boolean readyPlayerTwo,
      final PlayerId firstManoOfGame, final Round currentRound) {

    final var match = new Match(id, playerOne, playerTwo, inviteCode, rules, visibility, status);
    match.gamesWonPlayerOne = gamesWonPlayerOne;
    match.gamesWonPlayerTwo = gamesWonPlayerTwo;
    match.gameNumber = gameNumber;
    match.scorePlayerOne = scorePlayerOne;
    match.scorePlayerTwo = scorePlayerTwo;
    match.roundNumber = roundNumber;
    match.readyPlayerOne = readyPlayerOne;
    match.readyPlayerTwo = readyPlayerTwo;
    match.firstManoOfGame = firstManoOfGame;
    match.currentRound = currentRound;
    return match;
  }

  public void join(final PlayerId playerTwo, final InviteCode inviteCode) {

    Objects.requireNonNull(playerTwo, "PlayerTwo cannot be null");
    Objects.requireNonNull(inviteCode, "InviteCode cannot be null");

    if (this.status != MatchStatus.WAITING_FOR_PLAYERS) {
      throw new InvalidMatchStateException(this.status, MatchStatus.WAITING_FOR_PLAYERS);
    }

    if (!inviteCode.equals(this.inviteCode)) {
      throw new InvalidInviteCodeException();
    }

    if (playerTwo.equals(this.playerOne)) {
      throw new SamePlayerMatchException();
    }

    this.playerTwo = playerTwo;
    this.status = MatchStatus.READY;
    LOGGER.info("Player joined match: matchId={}, playerTwo={}", this.id, playerTwo);
    this.addDomainEvent(new PlayerJoinedEvent(this.id, this.playerOne, this.playerTwo));
  }

  public void joinPublic(final PlayerId playerTwo) {

    Objects.requireNonNull(playerTwo, "PlayerTwo cannot be null");

    if (this.visibility != Visibility.PUBLIC) {
      throw new PrivateMatchVisibilityAccessException();
    }

    if (this.status != MatchStatus.WAITING_FOR_PLAYERS) {
      throw new PublicMatchLobbyUnavailableException();
    }

    if (playerTwo.equals(this.playerOne)) {
      throw new SamePlayerMatchException();
    }

    this.playerTwo = playerTwo;
    this.status = MatchStatus.READY;
    LOGGER.info("Player joined public match: matchId={}, playerTwo={}", this.id, playerTwo);
    this.addDomainEvent(new PlayerJoinedEvent(this.id, this.playerOne, this.playerTwo));

    this.readyPlayerOne = true;
    this.readyPlayerTwo = true;
    this.startInternal();
  }

  public void startMatch(final PlayerId playerId) {

    Objects.requireNonNull(playerId, "PlayerId cannot be null");

    if (this.status == MatchStatus.IN_PROGRESS) {
      LOGGER.debug(
          "startMatch ignored because match is already in progress: matchId={}, playerId={}",
          this.id, playerId);
      return;
    }

    if (this.status == MatchStatus.WAITING_FOR_PLAYERS) {
      throw new MatchNotFullException();
    }

    if (this.status != MatchStatus.READY) {
      throw new InvalidMatchStateException(this.status, MatchStatus.READY);
    }

    this.validatePlayerInMatch(playerId);

    final var readyState = MatchReadyPolicy.markReady(this.readyPlayerOne, this.readyPlayerTwo,
        playerId.equals(this.playerOne));

    if (!readyState.changed()) {
      LOGGER.debug("Player already marked ready: matchId={}, playerId={}", this.id, playerId);
      return;
    }

    this.readyPlayerOne = readyState.readyPlayerOne();
    this.readyPlayerTwo = readyState.readyPlayerTwo();

    this.addDomainEvent(
        new PlayerReadyEvent(this.id, this.playerOne, this.playerTwo, this.seatOf(playerId)));

    if (readyState.bothReady()) {
      this.startInternal();
    }
  }

  public void playCard(final PlayerId playerId, final Card card) {

    this.validateCanActInCurrentRound(playerId);

    this.currentRound.playCard(playerId, card);
    LOGGER.debug("Card played: matchId={}, playerId={}, card={}", this.id, playerId, card);
    this.collectRoundEvents();

    this.currentRound.getRoundWinner().ifPresent(winner -> {
      final var newGameStarted = this.addGamePoints(winner,
          this.currentRound.getTrucoPointsAtStake());
      this.startNewRoundIfNeeded(newGameStarted);
    });
  }

  public void callTruco(final PlayerId playerId) {

    this.validateCanActInCurrentRound(playerId);

    this.currentRound.callTruco(playerId);
    LOGGER.info("Truco called: matchId={}, playerId={}", this.id, playerId);
    this.collectRoundEvents();
  }

  public void acceptTruco(final PlayerId playerId) {

    this.validateCanActInCurrentRound(playerId);

    this.currentRound.acceptTruco(playerId);
    LOGGER.info("Truco accepted: matchId={}, playerId={}", this.id, playerId);
    this.collectRoundEvents();
  }

  public void rejectTruco(final PlayerId playerId) {

    this.validateCanActInCurrentRound(playerId);

    final var result = this.currentRound.rejectTruco(playerId);
    LOGGER.info("Truco rejected: matchId={}, playerId={}, winner={}, points={}", this.id, playerId,
        result.winner(), result.points());
    this.collectRoundEvents();

    final var newGameStarted = this.addGamePoints(result.winner(), result.points());
    this.startNewRoundIfNeeded(newGameStarted);
  }

  public void acceptTrucoAndFold(final PlayerId playerId) {

    this.validateCanActInCurrentRound(playerId);

    final var result = this.currentRound.acceptTrucoAndFold(playerId);
    LOGGER.info(
        "Player folded after accepting truco: matchId={}, playerId={}, winner={}, points={}",
        this.id, playerId, result.winner(), result.points());
    this.collectRoundEvents();

    final var newGameStarted = this.addGamePoints(result.winner(), result.points());
    this.startNewRoundIfNeeded(newGameStarted);
  }

  public void fold(final PlayerId playerId) {

    this.validateCanActInCurrentRound(playerId);

    final var result = this.currentRound.fold(playerId);
    LOGGER.info("Player folded: matchId={}, playerId={}, winner={}, points={}", this.id, playerId,
        result.winner(), result.points());
    this.collectRoundEvents();

    final var newGameStarted = this.addGamePoints(result.winner(), result.points());
    this.startNewRoundIfNeeded(newGameStarted);
  }

  public void callEnvido(final PlayerId playerId, final EnvidoCall call) {

    this.validateCanActInCurrentRound(playerId);

    this.currentRound.callEnvido(playerId, call);
    LOGGER.info("Envido called: matchId={}, playerId={}, call={}", this.id, playerId, call);
    this.collectRoundEvents();
  }

  public void acceptEnvido(final PlayerId playerId) {

    this.validateCanActInCurrentRound(playerId);

    final var result = this.currentRound.acceptEnvido(playerId, this.scorePlayerOne,
        this.scorePlayerTwo);
    LOGGER.info("Envido accepted: matchId={}, playerId={}, winner={}, pointsWon={}", this.id,
        playerId, result.winner(), result.pointsWon());
    this.collectRoundEvents();

    this.addGamePoints(result.winner(), result.pointsWon());
  }

  public void rejectEnvido(final PlayerId playerId) {

    this.validateCanActInCurrentRound(playerId);

    final var result = this.currentRound.rejectEnvido(playerId);
    LOGGER.info("Envido rejected: matchId={}, playerId={}, winner={}, points={}", this.id, playerId,
        result.winner(), result.points());
    this.collectRoundEvents();

    this.addGamePoints(result.winner(), result.points());
  }

  public void leave(final PlayerId playerId) {

    Objects.requireNonNull(playerId, "PlayerId cannot be null");
    this.validatePlayerInMatch(playerId);

    if (this.status != MatchStatus.WAITING_FOR_PLAYERS && this.status != MatchStatus.READY) {
      throw new InvalidMatchStateException(this.status, MatchStatus.WAITING_FOR_PLAYERS,
          MatchStatus.READY);
    }

    if (playerId.equals(this.playerOne)) {
      this.status = MatchStatus.CANCELLED;
      LOGGER.info("Match cancelled by creator: matchId={}, creatorId={}", this.id, playerId);
      this.addDomainEvent(new MatchCancelledEvent(this.id, this.playerOne, this.playerTwo));
    } else {
      this.playerTwo = null;
      this.readyPlayerOne = false;
      this.readyPlayerTwo = false;
      this.status = MatchStatus.WAITING_FOR_PLAYERS;
      LOGGER.info("Player left match: matchId={}, playerId={}", this.id, playerId);
      this.addDomainEvent(new MatchPlayerLeftEvent(this.id, this.playerOne, PlayerSeat.PLAYER_TWO));
    }
  }

  public void cancel() {

    if (this.isFinished()) {
      return;
    }

    if (this.status != MatchStatus.WAITING_FOR_PLAYERS) {
      throw new InvalidMatchStateException(this.status, MatchStatus.WAITING_FOR_PLAYERS);
    }

    this.status = MatchStatus.CANCELLED;
    LOGGER.info("Match cancelled: matchId={}", this.id);
    this.addDomainEvent(new MatchCancelledEvent(this.id, this.playerOne, this.playerTwo));
  }

  public void abandon(final PlayerId abandoner) {

    Objects.requireNonNull(abandoner, "Abandoner cannot be null");
    this.validatePlayerInMatch(abandoner);

    if (this.status == MatchStatus.FINISHED || this.status == MatchStatus.CANCELLED) {
      return;
    }

    if (this.status != MatchStatus.IN_PROGRESS) {
      throw new InvalidMatchStateException(this.status, MatchStatus.IN_PROGRESS);
    }

    final var abandonerSeat = this.seatOf(abandoner);
    final var winnerSeat =
        abandonerSeat == PlayerSeat.PLAYER_ONE ? PlayerSeat.PLAYER_TWO : PlayerSeat.PLAYER_ONE;

    this.finishAdministrativeWin(winnerSeat);
    LOGGER.info("Match abandoned: matchId={}, abandoner={}, winner={}", this.id, abandoner,
        this.resolvePlayer(winnerSeat));
    this.addDomainEvent(
        new MatchAbandonedEvent(this.id, this.playerOne, this.playerTwo, winnerSeat, abandonerSeat,
            this.gamesWonPlayerOne, this.gamesWonPlayerTwo));
  }

  public boolean timeoutForfeit() {

    if (this.isFinished()) {
      return false;
    }
    if (this.status == MatchStatus.WAITING_FOR_PLAYERS) {
      this.cancel();
      return true;
    }
    final var winner = this.determineTimeoutWinner();
    if (winner == null) {
      return false;
    }
    this.forfeit(winner);
    return true;
  }

  private PlayerId determineTimeoutWinner() {

    if (this.status == MatchStatus.IN_PROGRESS) {
      final var currentTurn = this.getCurrentTurn();
      if (currentTurn == null) {
        return null;
      }
      return currentTurn.equals(this.playerOne) ? this.playerTwo : this.playerOne;
    }
    if (this.status == MatchStatus.READY) {
      if (this.playerTwo == null) {
        return null;
      }
      if (!this.readyPlayerOne) {
        return this.playerTwo;
      }
      if (!this.readyPlayerTwo) {
        return this.playerOne;
      }
      return this.playerTwo;
    }
    return null;
  }

  void forfeit(final PlayerId winner) {

    Objects.requireNonNull(winner, "Winner cannot be null");
    this.validatePlayerInMatch(winner);

    if (this.isFinished()) {
      return;
    }

    if (this.status == MatchStatus.WAITING_FOR_PLAYERS) {
      throw new InvalidMatchStateException(this.status, MatchStatus.READY);
    }

    final var winningSeat = this.seatOf(winner);
    this.finishAdministrativeWin(winningSeat);
    LOGGER.info("Match forfeited by timeout: matchId={}, winner={}", this.id, winner);
    this.addDomainEvent(
        new MatchForfeitedEvent(this.id, this.playerOne, this.playerTwo, winningSeat,
            this.gamesWonPlayerOne, this.gamesWonPlayerTwo));
  }

  private void finishAdministrativeWin(final PlayerSeat winningSeat) {

    if (winningSeat == PlayerSeat.PLAYER_ONE) {
      this.gamesWonPlayerOne = this.rules.gamesToWin();
      this.gamesWonPlayerTwo = Math.min(this.gamesWonPlayerTwo, this.rules.gamesToWin() - 1);
    } else {
      this.gamesWonPlayerTwo = this.rules.gamesToWin();
      this.gamesWonPlayerOne = Math.min(this.gamesWonPlayerOne, this.rules.gamesToWin() - 1);
    }

    this.status = MatchStatus.FINISHED;
    this.currentRound = null;
  }

  private PlayerId resolvePlayer(final PlayerSeat seat) {

    return seat == PlayerSeat.PLAYER_ONE ? this.playerOne : this.playerTwo;
  }

  private void startNewRoundIfNeeded(final boolean newGameStarted) {

    if (!newGameStarted && this.status == MatchStatus.IN_PROGRESS) {
      this.startNewRound();
    }
  }

  private void startNewGame() {

    final var state = MatchLifecyclePolicy.startNextGame(this.gameNumber, this.playerOne,
        this.playerTwo);
    this.gameNumber = state.gameNumber();
    this.scorePlayerOne = state.scorePlayerOne();
    this.scorePlayerTwo = state.scorePlayerTwo();
    this.roundNumber = state.roundNumber();
    this.firstManoOfGame = state.firstManoOfGame();

    LOGGER.info("Game started: matchId={}, gameNumber={}, firstMano={}", this.id, this.gameNumber,
        this.firstManoOfGame);
    this.addDomainEvent(
        new GameStartedEvent(this.id, this.playerOne, this.playerTwo, this.gameNumber));
    this.startNewRound();
  }

  private void startInternal() {

    this.status = MatchStatus.IN_PROGRESS;
    LOGGER.info("Match moved to IN_PROGRESS: matchId={}", this.id);
    this.startNewGame();
  }

  private void startNewRound() {

    final var nextRoundNumber = this.roundNumber + 1;
    final var mano = MatchLifecyclePolicy.resolveRoundMano(nextRoundNumber, this.firstManoOfGame,
        this.playerOne, this.playerTwo);
    this.roundNumber = nextRoundNumber;

    this.currentRound = Round.create(this.roundNumber, mano, this.playerOne, this.playerTwo);
    LOGGER.debug("Round started: matchId={}, gameNumber={}, roundNumber={}, mano={}", this.id,
        this.gameNumber, this.roundNumber, mano);
    this.collectRoundEvents();
  }

  private boolean addGamePoints(final PlayerId winner, final int points) {

    final var gameBefore = this.gameNumber;

    final var progression = MatchProgressionService.applyPoints(this.scorePlayerOne,
        this.scorePlayerTwo, this.gamesWonPlayerOne, this.gamesWonPlayerTwo, this.playerOne,
        this.playerTwo, this.rules, winner, points);

    this.scorePlayerOne = progression.scorePlayerOne();
    this.scorePlayerTwo = progression.scorePlayerTwo();

    LOGGER.info("Score changed: matchId={}, winner={}, points={}, score={} - {}", this.id, winner,
        points, this.scorePlayerOne, this.scorePlayerTwo);

    this.addDomainEvent(
        new ScoreChangedEvent(this.id, this.playerOne, this.playerTwo, this.scorePlayerOne,
            this.scorePlayerTwo));

    if (progression.gameOver()) {
      this.gamesWonPlayerOne = progression.gamesWonPlayerOne();
      this.gamesWonPlayerTwo = progression.gamesWonPlayerTwo();

      this.addDomainEvent(
          new GameScoreChangedEvent(this.id, this.playerOne, this.playerTwo, this.gamesWonPlayerOne,
              this.gamesWonPlayerTwo));

      LOGGER.info("Game resolved: matchId={}, gameWinner={}, gamesWon={} - {}", this.id,
          progression.gameWinner(), this.gamesWonPlayerOne, this.gamesWonPlayerTwo);

      if (progression.matchFinished()) {
        this.status = MatchStatus.FINISHED;
        this.currentRound = null;
        LOGGER.info("Match finished: matchId={}, winner={}, finalGames={} - {}", this.id,
            progression.gameWinner(), this.gamesWonPlayerOne, this.gamesWonPlayerTwo);
        this.addDomainEvent(new MatchFinishedEvent(this.id, this.playerOne, this.playerTwo,
            this.seatOf(progression.gameWinner()), this.gamesWonPlayerOne, this.gamesWonPlayerTwo));
      } else {
        this.startNewGame();
      }
    }

    return this.gameNumber != gameBefore;
  }

  private PlayerSeat seatOf(final PlayerId playerId) {

    return playerId.equals(this.playerOne) ? PlayerSeat.PLAYER_ONE : PlayerSeat.PLAYER_TWO;
  }

  private void collectRoundEvents() {

    if (this.currentRound != null) {
      RoundDomainEventDrain.drainFrom(this.currentRound).stream()
          .map(e -> new MatchEventEnvelope(this.id, this.playerOne, this.playerTwo, e))
          .forEach(this.domainEvents::add);
    }
  }

  public List<MatchDomainEvent> getMatchDomainEvents() {

    return getDomainEvents().stream().map(MatchDomainEvent.class::cast).toList();
  }

  private void validateMatchInProgress() {

    if (!MatchActionSpecification.canExecuteInRound(this.status)) {
      throw new InvalidMatchStateException(this.status, MatchStatus.IN_PROGRESS);
    }
  }

  public void validatePlayerInMatch(final PlayerId playerId) {

    if (!PlayerInMatchSpecification.isSatisfiedBy(playerId, this.playerOne, this.playerTwo)) {
      throw new PlayerNotInMatchException(playerId);
    }
  }

  private void validateCanActInCurrentRound(final PlayerId playerId) {

    this.validateMatchInProgress();
    this.validatePlayerInMatch(playerId);
  }

  @Override
  public MatchId getId() {

    return this.id;
  }

  public MatchStatus getStatus() {

    return this.status;
  }

  public PlayerId getPlayerOne() {

    return this.playerOne;
  }

  public PlayerId getPlayerTwo() {

    return this.playerTwo;
  }

  public Visibility getVisibility() {

    return this.visibility;
  }

  public InviteCode getInviteCode() {

    return this.inviteCode;
  }

  public int getGamesWonPlayerOne() {

    return this.gamesWonPlayerOne;
  }

  public int getGamesWonPlayerTwo() {

    return this.gamesWonPlayerTwo;
  }

  public int getScorePlayerOne() {

    return this.scorePlayerOne;
  }

  public int getScorePlayerTwo() {

    return this.scorePlayerTwo;
  }

  public boolean isFinished() {

    return this.status == MatchStatus.FINISHED || this.status == MatchStatus.CANCELLED;
  }

  public boolean isPublicLobbyOpen() {

    return this.visibility == Visibility.PUBLIC && this.status == MatchStatus.WAITING_FOR_PLAYERS;
  }

  public boolean isReadyPlayerOne() {

    return this.readyPlayerOne;
  }

  public boolean isReadyPlayerTwo() {

    return this.readyPlayerTwo;
  }

  public PlayerId getCurrentTurn() {

    return this.currentRound != null ? this.currentRound.getCurrentTurn() : null;
  }

  public RoundStatus getRoundStatus() {

    return this.currentRound != null ? this.currentRound.getStatus() : null;
  }

  public TrucoCall getCurrentTrucoCall() {

    return this.currentRound != null ? this.currentRound.getCurrentTrucoCall() : null;
  }

  List<EnvidoCall> getEnvidoChain() {

    return this.currentRound != null ? this.currentRound.getEnvidoStateMachine().getChain()
        : List.of();
  }

  public List<PlayedHandInfo> getPlayedHands() {

    if (this.currentRound == null) {
      return List.of();
    }
    return this.currentRound.getPlayedHands();
  }

  public CurrentHandInfo getCurrentHandInfo() {

    if (this.currentRound == null) {
      return new CurrentHandInfo(null, null, null);
    }

    return this.currentRound.getCurrentHandInfo();
  }

  public List<AvailableAction> getAvailableActions(final PlayerId playerId) {

    if (this.status != MatchStatus.IN_PROGRESS || this.currentRound == null) {
      return List.of();
    }
    return this.currentRound.getAvailableActions(playerId);
  }

  public MatchPlayerDecisionView getDecisionViewFor(final PlayerId playerId) {

    Objects.requireNonNull(playerId, "PlayerId cannot be null");
    this.validatePlayerInMatch(playerId);
    return MatchPlayerDecisionViewExtractor.extract(this, playerId);
  }

  public List<Card> getCardsOf(final PlayerId playerId) {

    if (this.status != MatchStatus.IN_PROGRESS) {
      return List.of();
    }
    return this.currentRound.getHandOf(playerId).getCards();
  }

  public PlayerId getMatchWinner() {

    if (this.status != MatchStatus.FINISHED) {
      return null;
    }
    return this.gamesWonPlayerOne >= this.rules.gamesToWin() ? this.playerOne : this.playerTwo;
  }

  Round getCurrentRound() {

    return this.currentRound;
  }

  MatchRules getRules() {

    return this.rules;
  }

  public int getGamesToPlay() {

    return this.rules.gamesToWin() * 2 - 1;
  }

  int getGameNumber() {

    return this.gameNumber;
  }

  int getRoundNumber() {

    return this.roundNumber;
  }

  PlayerId getFirstManoOfGame() {

    return this.firstManoOfGame;
  }

}
