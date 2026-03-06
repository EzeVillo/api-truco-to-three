package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.events.GameStartedEvent;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import com.villo.truco.domain.model.match.events.PlayerReadyEvent;
import com.villo.truco.domain.model.match.events.ScoreChangedEvent;
import com.villo.truco.domain.model.match.exceptions.InvalidInviteCodeException;
import com.villo.truco.domain.model.match.exceptions.InvalidMatchStateException;
import com.villo.truco.domain.model.match.exceptions.PlayerNotInMatchException;
import com.villo.truco.domain.model.match.exceptions.SamePlayerMatchException;
import com.villo.truco.domain.model.match.valueobjects.AvailableAction;
import com.villo.truco.domain.model.match.valueobjects.Card;
import com.villo.truco.domain.model.match.valueobjects.CurrentHandInfo;
import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.InviteCode;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.model.match.valueobjects.PlayedHandInfo;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.model.match.valueobjects.RoundStatus;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.shared.AggregateBase;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Match extends AggregateBase<MatchId> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Match.class);

  private final MatchRules rules;

  private final PlayerId playerOne;
  private final PlayerId playerTwo;
  private final InviteCode inviteCode;
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
      final InviteCode inviteCode, final MatchRules rules) {

    super(id);
    this.playerOne = playerOne;
    this.playerTwo = playerTwo;
    this.inviteCode = inviteCode;
    this.rules = rules;
    this.status = MatchStatus.WAITING_FOR_PLAYERS;
    this.gamesWonPlayerOne = 0;
    this.gamesWonPlayerTwo = 0;
    this.gameNumber = 0;
    this.readyPlayerOne = false;
    this.readyPlayerTwo = false;
  }

  public static Match create(final PlayerId playerOne, final PlayerId playerTwo) {

    return create(playerOne, playerTwo, MatchRules.defaultRules());
  }

  public static Match create(final PlayerId playerOne, final PlayerId playerTwo,
      final MatchRules rules) {

    Objects.requireNonNull(playerOne, "PlayerOne cannot be null");
    Objects.requireNonNull(playerTwo, "PlayerTwo cannot be null");
    Objects.requireNonNull(rules, "MatchRules cannot be null");
    if (playerOne.equals(playerTwo)) {
      throw new SamePlayerMatchException();
    }
    final var match = new Match(MatchId.generate(), playerOne, playerTwo, InviteCode.generate(),
        rules);
    LOGGER.info(
        "Match created: matchId={}, playerOne={}, playerTwo={}, rules={{gamesToWin={}, pointsToWinGame={}}}",
        match.getId(), playerOne, playerTwo, rules.gamesToWin(), rules.pointsToWinGame());
    return match;
  }

  public void join(final InviteCode inviteCode) {

    Objects.requireNonNull(inviteCode, "InviteCode cannot be null");

    if (this.status != MatchStatus.WAITING_FOR_PLAYERS) {
      throw new InvalidMatchStateException(this.status, MatchStatus.WAITING_FOR_PLAYERS);
    }

    if (!inviteCode.equals(this.inviteCode)) {
      throw new InvalidInviteCodeException();
    }

    LOGGER.info("Player joined match: matchId={}", this.id);
    this.addDomainEvent(new PlayerJoinedEvent());
  }

  public void startMatch(final PlayerId playerId) {

    Objects.requireNonNull(playerId, "PlayerId cannot be null");

    if (this.status == MatchStatus.IN_PROGRESS) {
      LOGGER.debug(
          "startMatch ignored because match is already in progress: matchId={}, playerId={}",
          this.id, playerId);
      return;
    }

    if (this.status != MatchStatus.WAITING_FOR_PLAYERS) {
      throw new InvalidMatchStateException(this.status, MatchStatus.WAITING_FOR_PLAYERS);
    }

    this.validatePlayerInMatch(playerId);

    if (playerId.equals(this.playerOne)) {
      if (this.readyPlayerOne) {
        LOGGER.debug("Player already marked ready: matchId={}, playerId={}", this.id, playerId);
        return;
      }
      this.readyPlayerOne = true;
    } else {
      if (this.readyPlayerTwo) {
        LOGGER.debug("Player already marked ready: matchId={}, playerId={}", this.id, playerId);
        return;
      }
      this.readyPlayerTwo = true;
    }

    this.addDomainEvent(new PlayerReadyEvent(this.seatOf(playerId)));

    if (this.readyPlayerOne && this.readyPlayerTwo) {
      this.status = MatchStatus.IN_PROGRESS;
      LOGGER.info("Match moved to IN_PROGRESS: matchId={}", this.id);
      this.startNewGame();
    }
  }

  public void playCard(final PlayerId playerId, final Card card) {

    this.validateMatchInProgress();
    this.validatePlayerInMatch(playerId);

    this.currentRound.playCard(playerId, card);
    LOGGER.debug("Card played: matchId={}, playerId={}, card={}", this.id, playerId, card);
    this.collectRoundEvents();

    this.currentRound.getRoundWinner().ifPresent(winner -> {
      final var newGameStarted = this.addGamePoints(winner,
          this.currentRound.getTrucoPointsAtStake());

      if (!newGameStarted) {
        this.startNewRound();
      }
    });
  }

  public void callTruco(final PlayerId playerId) {

    this.validateMatchInProgress();
    this.validatePlayerInMatch(playerId);

    this.currentRound.callTruco(playerId);
    LOGGER.info("Truco called: matchId={}, playerId={}", this.id, playerId);
    this.collectRoundEvents();
  }

  public void acceptTruco(final PlayerId playerId) {

    this.validateMatchInProgress();
    this.validatePlayerInMatch(playerId);

    this.currentRound.acceptTruco(playerId);
    LOGGER.info("Truco accepted: matchId={}, playerId={}", this.id, playerId);
    this.collectRoundEvents();
  }

  public void rejectTruco(final PlayerId playerId) {

    this.validateMatchInProgress();
    this.validatePlayerInMatch(playerId);

    final var result = this.currentRound.rejectTruco(playerId);
    LOGGER.info("Truco rejected: matchId={}, playerId={}, winner={}, points={}", this.id, playerId,
        result.winner(), result.points());
    this.collectRoundEvents();

    final var newGameStarted = this.addGamePoints(result.winner(), result.points());

    if (!newGameStarted) {
      this.startNewRound();
    }
  }

  public void acceptTrucoAndFold(final PlayerId playerId) {

    this.validateMatchInProgress();
    this.validatePlayerInMatch(playerId);

    final var result = this.currentRound.acceptTrucoAndFold(playerId);
    LOGGER.info(
        "Player folded after accepting truco: matchId={}, playerId={}, winner={}, points={}",
        this.id, playerId, result.winner(), result.points());
    this.collectRoundEvents();

    final var newGameStarted = this.addGamePoints(result.winner(), result.points());

    if (!newGameStarted) {
      this.startNewRound();
    }
  }

  public void fold(final PlayerId playerId) {

    this.validateMatchInProgress();
    this.validatePlayerInMatch(playerId);

    final var result = this.currentRound.fold(playerId);
    LOGGER.info("Player folded: matchId={}, playerId={}, winner={}, points={}", this.id, playerId,
        result.winner(), result.points());
    this.collectRoundEvents();

    final var newGameStarted = this.addGamePoints(result.winner(), result.points());

    if (!newGameStarted) {
      this.startNewRound();
    }
  }

  public void callEnvido(final PlayerId playerId, final EnvidoCall call) {

    this.validateMatchInProgress();
    this.validatePlayerInMatch(playerId);

    this.currentRound.callEnvido(playerId, call);
    LOGGER.info("Envido called: matchId={}, playerId={}, call={}", this.id, playerId, call);
    this.collectRoundEvents();
  }

  public void acceptEnvido(final PlayerId playerId) {

    this.validateMatchInProgress();
    this.validatePlayerInMatch(playerId);

    final var result = this.currentRound.acceptEnvido(playerId, this.scorePlayerOne,
        this.scorePlayerTwo);
    LOGGER.info("Envido accepted: matchId={}, playerId={}, winner={}, pointsWon={}", this.id,
        playerId, result.winner(), result.pointsWon());
    this.collectRoundEvents();

    this.addGamePoints(result.winner(), result.pointsWon());
  }

  public void rejectEnvido(final PlayerId playerId) {

    this.validateMatchInProgress();
    this.validatePlayerInMatch(playerId);

    final var result = this.currentRound.rejectEnvido(playerId);
    LOGGER.info("Envido rejected: matchId={}, playerId={}, winner={}, points={}", this.id, playerId,
        result.winner(), result.points());
    this.collectRoundEvents();

    this.addGamePoints(result.winner(), result.points());
    this.checkGameFinished();
  }

  private void startNewGame() {

    this.gameNumber++;
    this.scorePlayerOne = 0;
    this.scorePlayerTwo = 0;
    this.roundNumber = 0;
    this.firstManoOfGame = this.gameNumber % 2 == 1 ? this.playerOne : this.playerTwo;
    LOGGER.info("Game started: matchId={}, gameNumber={}, firstMano={}", this.id, this.gameNumber,
        this.firstManoOfGame);
    this.addDomainEvent(new GameStartedEvent(this.gameNumber));
    this.startNewRound();
  }

  private void startNewRound() {

    this.roundNumber++;
    final var mano =
        this.roundNumber % 2 == 1 ? this.firstManoOfGame : this.getOpponent(this.firstManoOfGame);
    this.currentRound = Round.create(this.roundNumber, mano, this.playerOne, this.playerTwo,
        this.rules);
    LOGGER.debug("Round started: matchId={}, gameNumber={}, roundNumber={}, mano={}", this.id,
        this.gameNumber, this.roundNumber, mano);
    this.collectRoundEvents();
  }

  private boolean addGamePoints(final PlayerId winner, final int points) {

    final var gameBefore = this.gameNumber;

    if (winner.equals(this.playerOne)) {
      this.scorePlayerOne += points;
    } else {
      this.scorePlayerTwo += points;
    }

    LOGGER.info("Score changed: matchId={}, winner={}, points={}, score={} - {}", this.id, winner,
        points, this.scorePlayerOne, this.scorePlayerTwo);

    this.addDomainEvent(new ScoreChangedEvent(this.scorePlayerOne, this.scorePlayerTwo));
    this.checkGameFinished();
    return this.gameNumber != gameBefore;
  }

  private void checkGameFinished() {

    final var evaluation = ScoringPolicy.evaluate(this.scorePlayerOne, this.scorePlayerTwo,
        this.gamesWonPlayerOne, this.gamesWonPlayerTwo, this.playerOne, this.playerTwo, this.rules);

    if (!evaluation.isGameOver()) {
      return;
    }

    final var gameWinner = evaluation.gameWinner();

    if (gameWinner.equals(this.playerOne)) {
      this.gamesWonPlayerOne++;
    } else {
      this.gamesWonPlayerTwo++;
    }

    LOGGER.info("Game resolved: matchId={}, gameWinner={}, gamesWon={} - {}", this.id, gameWinner,
        this.gamesWonPlayerOne, this.gamesWonPlayerTwo);

    if (evaluation.matchFinished()) {
      this.status = MatchStatus.FINISHED;
      this.currentRound = null;
      LOGGER.info("Match finished: matchId={}, winner={}, finalGames={} - {}", this.id, gameWinner,
          this.gamesWonPlayerOne, this.gamesWonPlayerTwo);
      this.addDomainEvent(new MatchFinishedEvent(this.seatOf(gameWinner), this.gamesWonPlayerOne,
          this.gamesWonPlayerTwo));
    } else {
      this.startNewGame();
    }
  }

  private PlayerSeat seatOf(final PlayerId playerId) {

    return playerId.equals(this.playerOne) ? PlayerSeat.PLAYER_ONE : PlayerSeat.PLAYER_TWO;
  }

  private void collectRoundEvents() {

    if (this.currentRound != null) {
      this.domainEvents.addAll(this.currentRound.getDomainEvents());
      this.currentRound.clearDomainEvents();
    }
  }

  private PlayerId getOpponent(final PlayerId playerId) {

    return playerId.equals(this.playerOne) ? this.playerTwo : this.playerOne;
  }

  private void validateMatchInProgress() {

    if (this.status != MatchStatus.IN_PROGRESS) {
      throw new InvalidMatchStateException(this.status, MatchStatus.IN_PROGRESS);
    }
  }

  private void validatePlayerInMatch(final PlayerId playerId) {

    final var isPlayerOne = playerId.equals(this.playerOne);
    final var isPlayerTwo = playerId.equals(this.playerTwo);

    if (!isPlayerOne && !isPlayerTwo) {
      throw new PlayerNotInMatchException(playerId);
    }
  }

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

    return this.status == MatchStatus.FINISHED;
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

}