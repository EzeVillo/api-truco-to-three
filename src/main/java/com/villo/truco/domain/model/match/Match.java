package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.events.GameStartedEvent;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import com.villo.truco.domain.model.match.events.ScoreChangedEvent;
import com.villo.truco.domain.model.match.exceptions.InvalidInviteCodeException;
import com.villo.truco.domain.model.match.exceptions.InvalidMatchStateException;
import com.villo.truco.domain.model.match.exceptions.PlayerNotInMatchException;
import com.villo.truco.domain.model.match.exceptions.SamePlayerMatchException;
import com.villo.truco.domain.model.match.valueobjects.AvailableAction;
import com.villo.truco.domain.model.match.valueobjects.Card;
import com.villo.truco.domain.model.match.valueobjects.CurrentHandInfo;
import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.EnvidoResult;
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

public final class Match extends AggregateBase<MatchId> {

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
    return new Match(MatchId.generate(), playerOne, playerTwo, InviteCode.generate(), rules);
  }

  public void join(final InviteCode inviteCode) {

    Objects.requireNonNull(inviteCode, "InviteCode cannot be null");

    if (this.status != MatchStatus.WAITING_FOR_PLAYERS) {
      throw new InvalidMatchStateException(this.status, MatchStatus.WAITING_FOR_PLAYERS);
    }

    if (!inviteCode.equals(this.inviteCode)) {
      throw new InvalidInviteCodeException();
    }

    this.status = MatchStatus.IN_PROGRESS;
    this.addDomainEvent(new PlayerJoinedEvent());
    this.startNewGame();
  }

  public void playCard(final PlayerId playerId, final Card card) {

    this.validateMatchInProgress();
    this.validatePlayerInMatch(playerId);

    this.currentRound.playCard(playerId, card);
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
    this.collectRoundEvents();
  }

  public void acceptTruco(final PlayerId playerId) {

    this.validateMatchInProgress();
    this.validatePlayerInMatch(playerId);

    this.currentRound.acceptTruco(playerId);
    this.collectRoundEvents();
  }

  public void rejectTruco(final PlayerId playerId) {

    this.validateMatchInProgress();
    this.validatePlayerInMatch(playerId);

    final var result = this.currentRound.rejectTruco(playerId);
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
    this.collectRoundEvents();
  }

  public EnvidoResult acceptEnvido(final PlayerId playerId) {

    this.validateMatchInProgress();
    this.validatePlayerInMatch(playerId);

    final var result = this.currentRound.acceptEnvido(playerId, this.scorePlayerOne,
        this.scorePlayerTwo);
    this.collectRoundEvents();

    this.addGamePoints(result.winner(), result.pointsWon());

    return result;
  }

  public void rejectEnvido(final PlayerId playerId) {

    this.validateMatchInProgress();
    this.validatePlayerInMatch(playerId);

    final var result = this.currentRound.rejectEnvido(playerId);
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
    this.addDomainEvent(new GameStartedEvent(this.gameNumber));
    this.startNewRound();
  }

  private void startNewRound() {

    this.roundNumber++;
    final var mano =
        this.roundNumber % 2 == 1 ? this.firstManoOfGame : this.getOpponent(this.firstManoOfGame);
    this.currentRound = Round.create(this.roundNumber, mano, this.playerOne, this.playerTwo,
        this.rules);
    this.collectRoundEvents();
  }

  private boolean addGamePoints(final PlayerId winner, final int points) {

    final var gameBefore = this.gameNumber;

    if (winner.equals(this.playerOne)) {
      this.scorePlayerOne += points;
    } else {
      this.scorePlayerTwo += points;
    }

    this.addDomainEvent(new ScoreChangedEvent(this.scorePlayerOne, this.scorePlayerTwo));
    this.checkGameFinished();
    return this.gameNumber != gameBefore;
  }

  private void checkGameFinished() {

    final boolean oneExceeded = this.scorePlayerOne > this.rules.pointsToWinGame();
    final boolean twoExceeded = this.scorePlayerTwo > this.rules.pointsToWinGame();
    final boolean oneWon = this.scorePlayerOne == this.rules.pointsToWinGame();
    final boolean twoWon = this.scorePlayerTwo == this.rules.pointsToWinGame();

    final PlayerId gameWinner;
    if (oneExceeded) {
      gameWinner = this.playerTwo;
    } else if (twoExceeded) {
      gameWinner = this.playerOne;
    } else if (oneWon) {
      gameWinner = this.playerOne;
    } else if (twoWon) {
      gameWinner = this.playerTwo;
    } else {
      return;
    }

    if (gameWinner.equals(this.playerOne)) {
      this.gamesWonPlayerOne++;
    } else {
      this.gamesWonPlayerTwo++;
    }

    if (this.gamesWonPlayerOne >= this.rules.gamesToWin()
        || this.gamesWonPlayerTwo >= this.rules.gamesToWin()) {
      this.status = MatchStatus.FINISHED;
      this.currentRound = null;
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