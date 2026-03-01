package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.exceptions.InvalidMatchStateException;
import com.villo.truco.domain.model.match.exceptions.PlayerNotInMatchException;
import com.villo.truco.domain.model.match.exceptions.SamePlayerMatchException;
import com.villo.truco.domain.model.match.valueobjects.AvailableAction;
import com.villo.truco.domain.model.match.valueobjects.Card;
import com.villo.truco.domain.model.match.valueobjects.CurrentHandInfo;
import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.EnvidoResult;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.model.match.valueobjects.PlayedHandInfo;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import com.villo.truco.domain.model.match.valueobjects.RoundStatus;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.shared.AggregateBase;
import java.util.List;
import java.util.Objects;

public final class Match extends AggregateBase<MatchId> {

    private final MatchRules rules;

    private final PlayerId playerOne;
    private final PlayerId playerTwo;
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
        final MatchRules rules) {

        super(id);
        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
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
        return new Match(MatchId.generate(), playerOne, playerTwo, rules);
    }

    public void join(final PlayerId playerId) {

        Objects.requireNonNull(playerId, "PlayerId cannot be null");

        if (this.status != MatchStatus.WAITING_FOR_PLAYERS) {
            throw new InvalidMatchStateException(this.status, MatchStatus.WAITING_FOR_PLAYERS);
        }

        if (!playerId.equals(this.playerTwo)) {
            throw new PlayerNotInMatchException(playerId);
        }

        this.status = MatchStatus.IN_PROGRESS;
        this.startNewGame();
    }

    public void playCard(final PlayerId playerId, final Card card) {

        this.validateMatchInProgress();
        this.validatePlayerInMatch(playerId);

        this.currentRound.playCard(playerId, card);

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
    }

    public void acceptTruco(final PlayerId playerId) {

        this.validateMatchInProgress();
        this.validatePlayerInMatch(playerId);
        this.currentRound.acceptTruco(playerId);
    }

    public void rejectTruco(final PlayerId playerId) {

        this.validateMatchInProgress();
        this.validatePlayerInMatch(playerId);

        final var result = this.currentRound.rejectTruco(playerId);

        final var newGameStarted = this.addGamePoints(result.winner(), result.points());

        if (!newGameStarted) {
            this.startNewRound();
        }
    }

    public void acceptTrucoAndFold(final PlayerId playerId) {

        this.validateMatchInProgress();
        this.validatePlayerInMatch(playerId);

        final var result = this.currentRound.acceptTrucoAndFold(playerId);

        final var newGameStarted = this.addGamePoints(result.winner(), result.points());

        if (!newGameStarted) {
            this.startNewRound();
        }
    }

    public void fold(final PlayerId playerId) {

        this.validateMatchInProgress();
        this.validatePlayerInMatch(playerId);

        final var result = this.currentRound.fold(playerId);

        final var newGameStarted = this.addGamePoints(result.winner(), result.points());

        if (!newGameStarted) {
            this.startNewRound();
        }
    }

    public void callEnvido(final PlayerId playerId, final EnvidoCall call) {

        this.validateMatchInProgress();
        this.validatePlayerInMatch(playerId);
        this.currentRound.callEnvido(playerId, call);
    }

    public EnvidoResult acceptEnvido(final PlayerId playerId) {

        this.validateMatchInProgress();
        this.validatePlayerInMatch(playerId);

        final var result = this.currentRound.acceptEnvido(playerId, this.scorePlayerOne,
            this.scorePlayerTwo);

        this.addGamePoints(result.winner(), result.pointsWon());

        return result;
    }

    public void rejectEnvido(final PlayerId playerId) {

        this.validateMatchInProgress();
        this.validatePlayerInMatch(playerId);

        final var result = this.currentRound.rejectEnvido(playerId);

        this.addGamePoints(result.winner(), result.points());
        this.checkGameFinished();
    }

    private void startNewGame() {

        this.gameNumber++;
        this.scorePlayerOne = 0;
        this.scorePlayerTwo = 0;
        this.roundNumber = 0;
        this.firstManoOfGame = this.gameNumber % 2 == 1 ? this.playerOne : this.playerTwo;
        this.startNewRound();
    }

    private void startNewRound() {

        this.roundNumber++;
        final var mano =
            this.roundNumber % 2 == 1 ? this.firstManoOfGame
                : this.getOpponent(this.firstManoOfGame);
        this.currentRound = Round.create(mano, this.playerOne, this.playerTwo, this.rules);
    }

    private boolean addGamePoints(final PlayerId winner, final int points) {

        final var gameBefore = this.gameNumber;

        if (winner.equals(this.playerOne)) {
            this.scorePlayerOne += points;
        } else {
            this.scorePlayerTwo += points;
        }

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
        } else {
            this.startNewGame();
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