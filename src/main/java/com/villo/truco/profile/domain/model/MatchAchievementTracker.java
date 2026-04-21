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

  public enum UpdateType {
    GAME_STARTED,
    ROUND_STARTED,
    ENVIDO_CALLED,
    ENVIDO_RESOLVED,
    TRUCO_CALLED,
    TRUCO_RESPONDED,
    HAND_RESOLVED,
    FOLDED,
    ROUND_ENDED,
    SCORE_CHANGED,
    OTHER
  }

  private final PlayerId playerOne;
  private final PlayerId playerTwo;
  private final List<EnvidoCall> envidoCallsInRound = new ArrayList<>();

  private boolean humanVsHuman;
  private int currentGameNumber;
  private int currentRoundNumber;
  private PlayerSeat manoSeat;

  private int previousScorePlayerOne;
  private int previousScorePlayerTwo;
  private int scorePlayerOne;
  private int scorePlayerTwo;

  private int playedHandsInRound;
  private boolean roundHadCalls;

  private EnvidoResponse lastEnvidoResponse;
  private PlayerSeat lastEnvidoWinnerSeat;
  private Integer lastEnvidoPointsMano;
  private Integer lastEnvidoPointsPie;

  private TrucoResponse lastTrucoResponse;
  private TrucoCall lastTrucoResponseCall;
  private PlayerSeat lastTrucoResponderSeat;

  private Card lastHandCardPlayerOne;
  private Card lastHandCardPlayerTwo;
  private PlayerSeat lastHandWinnerSeat;

  private PlayerSeat lastFoldedSeat;
  private PlayerSeat lastRoundWinnerSeat;

  private boolean pendingAcceptedValeCuatro;
  private boolean playerOneLostAcceptedValeCuatroByBust;
  private boolean playerTwoLostAcceptedValeCuatroByBust;

  private UpdateType lastUpdateType = UpdateType.OTHER;

  private MatchAchievementTracker(final MatchId matchId, final PlayerId playerOne,
      final PlayerId playerTwo, final boolean humanVsHuman) {

    super(Objects.requireNonNull(matchId));
    this.playerOne = Objects.requireNonNull(playerOne);
    this.playerTwo = Objects.requireNonNull(playerTwo);
    this.humanVsHuman = humanVsHuman;
  }

  public static MatchAchievementTracker create(final MatchId matchId, final PlayerId playerOne,
      final PlayerId playerTwo, final boolean humanVsHuman) {

    return new MatchAchievementTracker(matchId, playerOne, playerTwo, humanVsHuman);
  }

  static MatchAchievementTracker reconstruct(final MatchAchievementTrackerSnapshot snapshot) {

    final var tracker = new MatchAchievementTracker(snapshot.matchId(), snapshot.playerOne(),
        snapshot.playerTwo(), snapshot.humanVsHuman());
    tracker.currentGameNumber = snapshot.currentGameNumber();
    tracker.currentRoundNumber = snapshot.currentRoundNumber();
    tracker.manoSeat = snapshot.manoSeat();
    tracker.previousScorePlayerOne = snapshot.previousScorePlayerOne();
    tracker.previousScorePlayerTwo = snapshot.previousScorePlayerTwo();
    tracker.scorePlayerOne = snapshot.scorePlayerOne();
    tracker.scorePlayerTwo = snapshot.scorePlayerTwo();
    tracker.playedHandsInRound = snapshot.playedHandsInRound();
    tracker.roundHadCalls = snapshot.roundHadCalls();
    tracker.envidoCallsInRound.addAll(snapshot.envidoCallsInRound());
    tracker.lastEnvidoResponse = snapshot.lastEnvidoResponse();
    tracker.lastEnvidoWinnerSeat = snapshot.lastEnvidoWinnerSeat();
    tracker.lastEnvidoPointsMano = snapshot.lastEnvidoPointsMano();
    tracker.lastEnvidoPointsPie = snapshot.lastEnvidoPointsPie();
    tracker.lastTrucoResponse = snapshot.lastTrucoResponse();
    tracker.lastTrucoResponseCall = snapshot.lastTrucoResponseCall();
    tracker.lastTrucoResponderSeat = snapshot.lastTrucoResponderSeat();
    tracker.lastHandCardPlayerOne = snapshot.lastHandCardPlayerOne();
    tracker.lastHandCardPlayerTwo = snapshot.lastHandCardPlayerTwo();
    tracker.lastHandWinnerSeat = snapshot.lastHandWinnerSeat();
    tracker.lastFoldedSeat = snapshot.lastFoldedSeat();
    tracker.lastRoundWinnerSeat = snapshot.lastRoundWinnerSeat();
    tracker.pendingAcceptedValeCuatro = snapshot.pendingAcceptedValeCuatro();
    tracker.playerOneLostAcceptedValeCuatroByBust = snapshot.playerOneLostAcceptedValeCuatroByBust();
    tracker.playerTwoLostAcceptedValeCuatroByBust = snapshot.playerTwoLostAcceptedValeCuatroByBust();
    return tracker;
  }

  public void onGameStarted(final int gameNumber) {

    if (gameNumber <= 0) {
      throw new IllegalArgumentException("gameNumber must be positive");
    }
    this.currentGameNumber = gameNumber;
    this.currentRoundNumber = 0;
    this.manoSeat = null;
    this.previousScorePlayerOne = 0;
    this.previousScorePlayerTwo = 0;
    this.scorePlayerOne = 0;
    this.scorePlayerTwo = 0;
    this.resetRoundState();
    this.lastUpdateType = UpdateType.GAME_STARTED;
  }

  public void onRoundStarted(final int roundNumber, final PlayerSeat manoSeat) {

    this.currentRoundNumber = roundNumber;
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

  public void onTrucoCalled(final TrucoCall call) {

    this.roundHadCalls = true;
    this.lastUpdateType = UpdateType.TRUCO_CALLED;
  }

  public void onTrucoResponded(final PlayerSeat responderSeat, final TrucoResponse response,
      final TrucoCall call) {

    this.lastTrucoResponderSeat = Objects.requireNonNull(responderSeat);
    this.lastTrucoResponse = Objects.requireNonNull(response);
    this.lastTrucoResponseCall = Objects.requireNonNull(call);
    this.pendingAcceptedValeCuatro = response == TrucoResponse.QUIERO
        && call == TrucoCall.VALE_CUATRO;
    this.lastUpdateType = UpdateType.TRUCO_RESPONDED;
  }

  public void onHandResolved(final Card cardPlayerOne, final Card cardPlayerTwo,
      final PlayerSeat winnerSeat) {

    this.playedHandsInRound += 1;
    this.lastHandCardPlayerOne = cardPlayerOne;
    this.lastHandCardPlayerTwo = cardPlayerTwo;
    this.lastHandWinnerSeat = winnerSeat;
    this.lastUpdateType = UpdateType.HAND_RESOLVED;
  }

  public void onFolded(final PlayerSeat foldedSeat) {

    this.lastFoldedSeat = Objects.requireNonNull(foldedSeat);
    this.lastUpdateType = UpdateType.FOLDED;
  }

  public void onRoundEnded(final PlayerSeat winnerSeat) {

    this.lastRoundWinnerSeat = Objects.requireNonNull(winnerSeat);
    this.lastUpdateType = UpdateType.ROUND_ENDED;
  }

  public void onScoreChanged(final int scorePlayerOne, final int scorePlayerTwo) {

    this.previousScorePlayerOne = this.scorePlayerOne;
    this.previousScorePlayerTwo = this.scorePlayerTwo;
    this.scorePlayerOne = scorePlayerOne;
    this.scorePlayerTwo = scorePlayerTwo;
    if (this.pendingAcceptedValeCuatro) {
      if (this.scorePlayerOne > 3) {
        this.playerOneLostAcceptedValeCuatroByBust = true;
      }
      if (this.scorePlayerTwo > 3) {
        this.playerTwoLostAcceptedValeCuatroByBust = true;
      }
      this.pendingAcceptedValeCuatro = false;
    }
    this.lastUpdateType = UpdateType.SCORE_CHANGED;
  }

  public void onOtherEvent() {

    this.lastUpdateType = UpdateType.OTHER;
  }

  private void resetRoundState() {

    this.playedHandsInRound = 0;
    this.roundHadCalls = false;
    this.envidoCallsInRound.clear();
    this.lastEnvidoResponse = null;
    this.lastEnvidoWinnerSeat = null;
    this.lastEnvidoPointsMano = null;
    this.lastEnvidoPointsPie = null;
    this.lastTrucoResponse = null;
    this.lastTrucoResponseCall = null;
    this.lastTrucoResponderSeat = null;
    this.lastHandCardPlayerOne = null;
    this.lastHandCardPlayerTwo = null;
    this.lastHandWinnerSeat = null;
    this.lastFoldedSeat = null;
    this.lastRoundWinnerSeat = null;
    this.pendingAcceptedValeCuatro = false;
  }

  public MatchAchievementTrackerSnapshot snapshot() {

    return new MatchAchievementTrackerSnapshot(this.id, this.playerOne, this.playerTwo,
        this.humanVsHuman, this.currentGameNumber, this.currentRoundNumber, this.manoSeat,
        this.previousScorePlayerOne, this.previousScorePlayerTwo, this.scorePlayerOne,
        this.scorePlayerTwo, this.playedHandsInRound, this.roundHadCalls,
        List.copyOf(this.envidoCallsInRound), this.lastEnvidoResponse, this.lastEnvidoWinnerSeat,
        this.lastEnvidoPointsMano, this.lastEnvidoPointsPie, this.lastTrucoResponse,
        this.lastTrucoResponseCall, this.lastTrucoResponderSeat, this.lastHandCardPlayerOne,
        this.lastHandCardPlayerTwo, this.lastHandWinnerSeat, this.lastFoldedSeat,
        this.lastRoundWinnerSeat, this.pendingAcceptedValeCuatro,
        this.playerOneLostAcceptedValeCuatroByBust, this.playerTwoLostAcceptedValeCuatroByBust);
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

    return seat == PlayerSeat.PLAYER_ONE ? this.previousScorePlayerOne : this.previousScorePlayerTwo;
  }

  public boolean hasLostAcceptedValeCuatroByBust(final PlayerSeat seat) {

    return seat == PlayerSeat.PLAYER_ONE ? this.playerOneLostAcceptedValeCuatroByBust
        : this.playerTwoLostAcceptedValeCuatroByBust;
  }

  public PlayerId getPlayerOne() {

    return this.playerOne;
  }

  public PlayerId getPlayerTwo() {

    return this.playerTwo;
  }

  public boolean isHumanVsHuman() {

    return this.humanVsHuman;
  }

  public int getCurrentGameNumber() {

    return this.currentGameNumber;
  }

  public int getCurrentRoundNumber() {

    return this.currentRoundNumber;
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

  public int getPlayedHandsInRound() {

    return this.playedHandsInRound;
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

  public PlayerSeat getLastRoundWinnerSeat() {

    return this.lastRoundWinnerSeat;
  }

  public boolean isPendingAcceptedValeCuatro() {

    return this.pendingAcceptedValeCuatro;
  }

  public boolean isPlayerOneLostAcceptedValeCuatroByBust() {

    return this.playerOneLostAcceptedValeCuatroByBust;
  }

  public boolean isPlayerTwoLostAcceptedValeCuatroByBust() {

    return this.playerTwoLostAcceptedValeCuatroByBust;
  }

  public UpdateType getLastUpdateType() {

    return this.lastUpdateType;
  }
}
