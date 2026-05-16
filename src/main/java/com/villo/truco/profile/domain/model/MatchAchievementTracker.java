package com.villo.truco.profile.domain.model;

import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.EnvidoResponse;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.model.match.valueobjects.TrucoCall;
import com.villo.truco.domain.model.match.valueobjects.TrucoResponse;
import com.villo.truco.domain.shared.AggregateBase;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MatchAchievementTracker extends AggregateBase<MatchId> {

  private final PlayerId playerOne;
  private final PlayerId playerTwo;
  private final List<EnvidoCall> envidoCallsInRound = new ArrayList<>();
  private int currentGameNumber;
  private PlayerSeat manoSeat;
  private int previousScorePlayerOne;
  private int previousScorePlayerTwo;
  private int scorePlayerOne;
  private int scorePlayerTwo;
  private int playedHandsInRound;
  private int cardsPlayedInRound;
  private boolean roundHadCalls;
  private EnvidoResponse lastEnvidoResponse;
  private PlayerSeat lastEnvidoWinnerSeat;
  private Integer lastEnvidoPointsMano;
  private Integer lastEnvidoPointsPie;
  private TrucoResponse lastTrucoResponse;
  private TrucoCall lastTrucoResponseCall;
  private PlayerSeat lastTrucoResponderSeat;
  private PlayerSeat lastTrucoCallerSeat;
  private boolean playerOnePlayedCurrentHand;
  private boolean playerTwoPlayedCurrentHand;
  private boolean trucoCalledWhenRivalHadNoCards;
  private Card lastHandCardPlayerOne;
  private Card lastHandCardPlayerTwo;
  private PlayerSeat lastHandWinnerSeat;
  private PlayerSeat lastFoldedSeat;
  private UpdateType lastUpdateType = UpdateType.OTHER;

  private MatchAchievementTracker(final MatchId matchId, final PlayerId playerOne,
      final PlayerId playerTwo) {

    super(Objects.requireNonNull(matchId));
    this.playerOne = Objects.requireNonNull(playerOne);
    this.playerTwo = Objects.requireNonNull(playerTwo);
  }

  public static MatchAchievementTracker create(final MatchId matchId, final PlayerId playerOne,
      final PlayerId playerTwo) {

    return new MatchAchievementTracker(matchId, playerOne, playerTwo);
  }

  static MatchAchievementTracker reconstruct(final MatchAchievementTrackerSnapshot snapshot) {

    final var tracker = new MatchAchievementTracker(snapshot.matchId(), snapshot.playerOne(),
        snapshot.playerTwo());
    tracker.currentGameNumber = snapshot.currentGameNumber();
    tracker.manoSeat = snapshot.manoSeat();
    tracker.previousScorePlayerOne = snapshot.previousScorePlayerOne();
    tracker.previousScorePlayerTwo = snapshot.previousScorePlayerTwo();
    tracker.scorePlayerOne = snapshot.scorePlayerOne();
    tracker.scorePlayerTwo = snapshot.scorePlayerTwo();
    tracker.playedHandsInRound = snapshot.playedHandsInRound();
    tracker.cardsPlayedInRound = snapshot.cardsPlayedInRound();
    tracker.roundHadCalls = snapshot.roundHadCalls();
    tracker.envidoCallsInRound.addAll(snapshot.envidoCallsInRound());
    tracker.lastEnvidoResponse = snapshot.lastEnvidoResponse();
    tracker.lastEnvidoWinnerSeat = snapshot.lastEnvidoWinnerSeat();
    tracker.lastEnvidoPointsMano = snapshot.lastEnvidoPointsMano();
    tracker.lastEnvidoPointsPie = snapshot.lastEnvidoPointsPie();
    tracker.lastTrucoResponse = snapshot.lastTrucoResponse();
    tracker.lastTrucoResponseCall = snapshot.lastTrucoResponseCall();
    tracker.lastTrucoResponderSeat = snapshot.lastTrucoResponderSeat();
    tracker.lastTrucoCallerSeat = snapshot.lastTrucoCallerSeat();
    tracker.playerOnePlayedCurrentHand = snapshot.playerOnePlayedCurrentHand();
    tracker.playerTwoPlayedCurrentHand = snapshot.playerTwoPlayedCurrentHand();
    tracker.trucoCalledWhenRivalHadNoCards = snapshot.trucoCalledWhenRivalHadNoCards();
    tracker.lastHandCardPlayerOne = snapshot.lastHandCardPlayerOne();
    tracker.lastHandCardPlayerTwo = snapshot.lastHandCardPlayerTwo();
    tracker.lastHandWinnerSeat = snapshot.lastHandWinnerSeat();
    tracker.lastFoldedSeat = snapshot.lastFoldedSeat();
    return tracker;
  }

  public void onGameStarted(final int gameNumber) {

    if (gameNumber <= 0) {
      throw new IllegalArgumentException("gameNumber must be positive");
    }
    this.currentGameNumber = gameNumber;
    this.manoSeat = null;
    this.previousScorePlayerOne = 0;
    this.previousScorePlayerTwo = 0;
    this.scorePlayerOne = 0;
    this.scorePlayerTwo = 0;
    this.resetRoundState();
    this.lastUpdateType = UpdateType.GAME_STARTED;
  }

  public void onRoundStarted(final PlayerSeat manoSeat) {

    this.manoSeat = Objects.requireNonNull(manoSeat);
    this.resetRoundState();
    this.lastUpdateType = UpdateType.ROUND_STARTED;
  }

  public void onEnvidoCalled(final EnvidoCall call) {

    this.roundHadCalls = true;
    this.envidoCallsInRound.add(Objects.requireNonNull(call));
    this.lastUpdateType = UpdateType.ENVIDO_CALLED;
  }

  public void onEnvidoResolved(final EnvidoResponse response, final PlayerSeat winnerSeat,
      final Integer pointsMano, final Integer pointsPie) {

    this.lastEnvidoResponse = Objects.requireNonNull(response);
    this.lastEnvidoWinnerSeat = Objects.requireNonNull(winnerSeat);
    this.lastEnvidoPointsMano = pointsMano;
    this.lastEnvidoPointsPie = pointsPie;
    this.lastUpdateType = UpdateType.ENVIDO_RESOLVED;
  }

  public void onTrucoCalled(final PlayerSeat callerSeat) {

    this.roundHadCalls = true;
    this.lastTrucoCallerSeat = Objects.requireNonNull(callerSeat);
    this.trucoCalledWhenRivalHadNoCards =
        this.playedHandsInRound == 2 && this.playerPlayedCurrentHand(this.opposite(callerSeat));
    this.lastUpdateType = UpdateType.TRUCO_CALLED;
  }

  public void onTrucoResponded(final PlayerSeat responderSeat, final TrucoResponse response,
      final TrucoCall call) {

    this.lastTrucoResponderSeat = Objects.requireNonNull(responderSeat);
    this.lastTrucoResponse = Objects.requireNonNull(response);
    this.lastTrucoResponseCall = Objects.requireNonNull(call);
    this.lastUpdateType = UpdateType.TRUCO_RESPONDED;
  }

  public void onHandResolved(final Card cardPlayerOne, final Card cardPlayerTwo,
      final PlayerSeat winnerSeat) {

    this.playedHandsInRound += 1;
    this.lastHandCardPlayerOne = cardPlayerOne;
    this.lastHandCardPlayerTwo = cardPlayerTwo;
    this.lastHandWinnerSeat = winnerSeat;
    this.playerOnePlayedCurrentHand = false;
    this.playerTwoPlayedCurrentHand = false;
    this.lastUpdateType = UpdateType.HAND_RESOLVED;
  }

  public void onCardPlayed(final PlayerSeat seat) {

    Objects.requireNonNull(seat);
    this.cardsPlayedInRound += 1;
    if (seat == PlayerSeat.PLAYER_ONE) {
      this.playerOnePlayedCurrentHand = true;
    } else {
      this.playerTwoPlayedCurrentHand = true;
    }
  }

  public void onFolded(final PlayerSeat foldedSeat) {

    this.lastFoldedSeat = Objects.requireNonNull(foldedSeat);
    this.lastUpdateType = UpdateType.FOLDED;
  }

  public void onScoreChanged(final int scorePlayerOne, final int scorePlayerTwo) {

    this.previousScorePlayerOne = this.scorePlayerOne;
    this.previousScorePlayerTwo = this.scorePlayerTwo;
    this.scorePlayerOne = scorePlayerOne;
    this.scorePlayerTwo = scorePlayerTwo;
    this.lastUpdateType = UpdateType.SCORE_CHANGED;
  }

  public void onOtherEvent() {

    this.lastUpdateType = UpdateType.OTHER;
  }

  private void resetRoundState() {

    this.playedHandsInRound = 0;
    this.cardsPlayedInRound = 0;
    this.roundHadCalls = false;
    this.envidoCallsInRound.clear();
    this.lastEnvidoResponse = null;
    this.lastEnvidoWinnerSeat = null;
    this.lastEnvidoPointsMano = null;
    this.lastEnvidoPointsPie = null;
    this.lastTrucoResponse = null;
    this.lastTrucoResponseCall = null;
    this.lastTrucoResponderSeat = null;
    this.lastTrucoCallerSeat = null;
    this.lastHandCardPlayerOne = null;
    this.lastHandCardPlayerTwo = null;
    this.lastHandWinnerSeat = null;
    this.lastFoldedSeat = null;
    this.playerOnePlayedCurrentHand = false;
    this.playerTwoPlayedCurrentHand = false;
    this.trucoCalledWhenRivalHadNoCards = false;
  }

  public MatchAchievementTrackerSnapshot snapshot() {

    return new MatchAchievementTrackerSnapshot(this.id, this.playerOne, this.playerTwo,
        this.currentGameNumber, this.manoSeat, this.previousScorePlayerOne,
        this.previousScorePlayerTwo, this.scorePlayerOne, this.scorePlayerTwo,
        this.playedHandsInRound, this.cardsPlayedInRound, this.roundHadCalls,
        List.copyOf(this.envidoCallsInRound), this.lastEnvidoResponse, this.lastEnvidoWinnerSeat,
        this.lastEnvidoPointsMano, this.lastEnvidoPointsPie, this.lastTrucoResponse,
        this.lastTrucoResponseCall, this.lastTrucoResponderSeat, this.lastTrucoCallerSeat,
        this.lastHandCardPlayerOne, this.lastHandCardPlayerTwo, this.lastHandWinnerSeat,
        this.lastFoldedSeat, this.playerOnePlayedCurrentHand, this.playerTwoPlayedCurrentHand,
        this.trucoCalledWhenRivalHadNoCards);
  }

  public PlayerId resolvePlayer(final PlayerSeat seat) {

    return seat == PlayerSeat.PLAYER_ONE ? this.playerOne : this.playerTwo;
  }

  public PlayerSeat opposite(final PlayerSeat seat) {

    return seat == PlayerSeat.PLAYER_ONE ? PlayerSeat.PLAYER_TWO : PlayerSeat.PLAYER_ONE;
  }

  public int scoreFor(final PlayerSeat seat) {

    return seat == PlayerSeat.PLAYER_ONE ? this.scorePlayerOne : this.scorePlayerTwo;
  }

  public int previousScoreFor(final PlayerSeat seat) {

    return seat == PlayerSeat.PLAYER_ONE ? this.previousScorePlayerOne
        : this.previousScorePlayerTwo;
  }

  public PlayerId getPlayerOne() {

    return this.playerOne;
  }

  public PlayerId getPlayerTwo() {

    return this.playerTwo;
  }

  public int getCurrentGameNumber() {

    return this.currentGameNumber;
  }

  public PlayerSeat getManoSeat() {

    return this.manoSeat;
  }

  public int getPreviousScorePlayerOne() {

    return this.previousScorePlayerOne;
  }

  public int getPreviousScorePlayerTwo() {

    return this.previousScorePlayerTwo;
  }

  public int getScorePlayerOne() {

    return this.scorePlayerOne;
  }

  public int getScorePlayerTwo() {

    return this.scorePlayerTwo;
  }

  public int getCardsPlayedInRound() {

    return this.cardsPlayedInRound;
  }

  public boolean isRoundHadCalls() {

    return this.roundHadCalls;
  }

  public List<EnvidoCall> getEnvidoCallsInRound() {

    return List.copyOf(this.envidoCallsInRound);
  }

  public EnvidoResponse getLastEnvidoResponse() {

    return this.lastEnvidoResponse;
  }

  public PlayerSeat getLastEnvidoWinnerSeat() {

    return this.lastEnvidoWinnerSeat;
  }

  public Integer getLastEnvidoPointsMano() {

    return this.lastEnvidoPointsMano;
  }

  public Integer getLastEnvidoPointsPie() {

    return this.lastEnvidoPointsPie;
  }

  public TrucoResponse getLastTrucoResponse() {

    return this.lastTrucoResponse;
  }

  public TrucoCall getLastTrucoResponseCall() {

    return this.lastTrucoResponseCall;
  }

  public PlayerSeat getLastTrucoResponderSeat() {

    return this.lastTrucoResponderSeat;
  }

  public PlayerSeat getLastTrucoCallerSeat() {

    return this.lastTrucoCallerSeat;
  }

  public boolean isTrucoCalledWhenRivalHadNoCards() {

    return this.trucoCalledWhenRivalHadNoCards;
  }

  private boolean playerPlayedCurrentHand(final PlayerSeat seat) {

    return seat == PlayerSeat.PLAYER_ONE ? this.playerOnePlayedCurrentHand
        : this.playerTwoPlayedCurrentHand;
  }

  public Card getLastHandCardPlayerOne() {

    return this.lastHandCardPlayerOne;
  }

  public Card getLastHandCardPlayerTwo() {

    return this.lastHandCardPlayerTwo;
  }

  public PlayerSeat getLastHandWinnerSeat() {

    return this.lastHandWinnerSeat;
  }

  public PlayerSeat getLastFoldedSeat() {

    return this.lastFoldedSeat;
  }

  public UpdateType getLastUpdateType() {

    return this.lastUpdateType;
  }

  public enum UpdateType {
    GAME_STARTED, ROUND_STARTED, ENVIDO_CALLED, ENVIDO_RESOLVED, TRUCO_CALLED, TRUCO_RESPONDED, HAND_RESOLVED, FOLDED, SCORE_CHANGED, OTHER
  }

}
