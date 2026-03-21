package com.villo.truco.domain.model.cup;

import com.villo.truco.domain.model.cup.MatchAdvancementResult.BoutPairing;
import com.villo.truco.domain.model.cup.exceptions.BoutAlreadyResolvedException;
import com.villo.truco.domain.model.cup.exceptions.BracketCorruptedException;
import com.villo.truco.domain.model.cup.exceptions.CupCreatorCannotLeaveException;
import com.villo.truco.domain.model.cup.exceptions.CupFullException;
import com.villo.truco.domain.model.cup.exceptions.CupNotReadyException;
import com.villo.truco.domain.model.cup.exceptions.CupNotWaitingException;
import com.villo.truco.domain.model.cup.exceptions.InvalidCupInviteCodeException;
import com.villo.truco.domain.model.cup.exceptions.InvalidCupPlayersException;
import com.villo.truco.domain.model.cup.exceptions.MatchNotPartOfCupException;
import com.villo.truco.domain.model.cup.exceptions.OnlyCreatorCanStartCupException;
import com.villo.truco.domain.model.cup.exceptions.PlayerAlreadyInCupException;
import com.villo.truco.domain.model.cup.exceptions.PlayerNotInCupException;
import com.villo.truco.domain.model.cup.exceptions.WinnerNotInBoutException;
import com.villo.truco.domain.model.cup.valueobjects.BoutId;
import com.villo.truco.domain.model.cup.valueobjects.BoutStatus;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.cup.valueobjects.CupStatus;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.AggregateBase;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Cup extends AggregateBase<CupId> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Cup.class);

  private final List<PlayerId> participants;
  private final List<Bout> bouts;
  private final Set<PlayerId> forfeitedPlayers;
  private final int numberOfPlayers;
  private final GamesToPlay gamesToPlay;
  private final InviteCode inviteCode;
  private CupStatus status;
  private PlayerId champion;

  private Cup(final CupId id, final List<PlayerId> participants, final List<Bout> bouts,
      final Set<PlayerId> forfeitedPlayers, final int numberOfPlayers,
      final GamesToPlay gamesToPlay, final InviteCode inviteCode, final CupStatus status,
      final PlayerId champion) {

    super(id);
    this.participants = participants;
    this.bouts = bouts;
    this.forfeitedPlayers = forfeitedPlayers;
    this.numberOfPlayers = numberOfPlayers;
    this.gamesToPlay = gamesToPlay;
    this.inviteCode = inviteCode;
    this.status = status;
    this.champion = champion;
  }

  public static Cup create(final PlayerId creatorId, final int numberOfPlayers,
      final GamesToPlay gamesToPlay) {

    Objects.requireNonNull(creatorId, "Creator cannot be null");
    Objects.requireNonNull(gamesToPlay, "GamesToPlay cannot be null");

    if (numberOfPlayers < 4 || numberOfPlayers > 8) {
      throw new InvalidCupPlayersException("Cup numberOfPlayers must be between 4 and 8");
    }

    final var participants = new ArrayList<PlayerId>();
    participants.add(creatorId);

    final var cup = new Cup(CupId.generate(), participants, new ArrayList<>(), new HashSet<>(),
        numberOfPlayers, gamesToPlay, InviteCode.generate(), CupStatus.WAITING_FOR_PLAYERS, null);
    LOGGER.info("Cup created: cupId={}, creator={}, numberOfPlayers={}, gamesToPlay={}",
        cup.getId(), creatorId, numberOfPlayers, gamesToPlay);
    return cup;
  }

  static Cup reconstruct(final CupId id, final List<PlayerId> participants, final List<Bout> bouts,
      final Set<PlayerId> forfeitedPlayers, final int numberOfPlayers,
      final GamesToPlay gamesToPlay, final InviteCode inviteCode, final CupStatus status,
      final PlayerId champion) {

    return new Cup(id, participants, bouts, forfeitedPlayers, numberOfPlayers, gamesToPlay,
        inviteCode, status, champion);
  }

  private static int nextPowerOfTwo(final int n) {

    if (n <= 1) {
      return 1;
    }
    return Integer.highestOneBit(n - 1) << 1;
  }

  public void join(final PlayerId playerId, final InviteCode inviteCode) {

    Objects.requireNonNull(playerId, "PlayerId cannot be null");
    Objects.requireNonNull(inviteCode, "InviteCode cannot be null");

    if (this.status != CupStatus.WAITING_FOR_PLAYERS) {
      throw new CupNotWaitingException();
    }

    if (!this.inviteCode.equals(inviteCode)) {
      throw new InvalidCupInviteCodeException();
    }

    if (this.participants.contains(playerId)) {
      throw new PlayerAlreadyInCupException();
    }

    if (this.participants.size() >= this.numberOfPlayers) {
      throw new CupFullException();
    }

    this.participants.add(playerId);
    LOGGER.info("Player joined cup: cupId={}, playerId={}, participants={}/{}", this.id, playerId,
        this.participants.size(), this.numberOfPlayers);

    if (this.participants.size() == this.numberOfPlayers) {
      this.status = CupStatus.WAITING_FOR_START;
      LOGGER.info("Cup ready: cupId={}, participants={}", this.id, this.participants.size());
    }
  }

  public void leave(final PlayerId playerId) {

    Objects.requireNonNull(playerId, "PlayerId cannot be null");

    if (this.status != CupStatus.WAITING_FOR_PLAYERS
        && this.status != CupStatus.WAITING_FOR_START) {
      throw new CupNotWaitingException();
    }

    if (!this.participants.contains(playerId)) {
      throw new PlayerNotInCupException();
    }

    if (this.participants.getFirst().equals(playerId)) {
      throw new CupCreatorCannotLeaveException();
    }

    this.participants.remove(playerId);
    this.status = CupStatus.WAITING_FOR_PLAYERS;
    LOGGER.info("Player left cup: cupId={}, playerId={}, participants={}/{}", this.id, playerId,
        this.participants.size(), this.numberOfPlayers);
  }

  public void start(final PlayerId playerId) {

    Objects.requireNonNull(playerId, "PlayerId cannot be null");

    if (this.status != CupStatus.WAITING_FOR_START) {
      throw new CupNotReadyException();
    }

    if (!this.participants.getFirst().equals(playerId)) {
      throw new OnlyCreatorCanStartCupException();
    }

    this.generateBracket();
    this.status = CupStatus.IN_PROGRESS;
    LOGGER.info("Cup started: cupId={}, participants={}, bouts={}", this.id,
        this.participants.size(), this.bouts.size());
  }

  private void generateBracket() {

    final var shuffled = new ArrayList<>(this.participants);
    Collections.shuffle(shuffled);

    final var n = shuffled.size();
    final var p = nextPowerOfTwo(n);
    final var totalRounds = Integer.numberOfTrailingZeros(p);
    final var pendingBouts = n - p / 2;
    final var byeBouts = p - n;

    for (var round = 1; round <= totalRounds; round++) {
      final var boutsInRound = p / (1 << round);
      for (var pos = 0; pos < boutsInRound; pos++) {
        this.bouts.add(Bout.awaiting(BoutId.generate(), round, pos));
      }
    }

    for (var i = 0; i < pendingBouts; i++) {
      final var bout = this.findBout(1, i);
      bout.assignPlayerOne(shuffled.get(2 * i));
      bout.assignPlayerTwo(shuffled.get(2 * i + 1));
      bout.transitionToPending();
    }

    for (var i = 0; i < byeBouts; i++) {
      final var boutPos = pendingBouts + i;
      final var player = shuffled.get(2 * pendingBouts + i);
      final var bout = this.findBout(1, boutPos);
      bout.resolveAsBye(player);
      this.advancePlayerToNextRound(1, boutPos, player);
    }
  }

  private void advancePlayerToNextRound(final int currentRound, final int currentPos,
      final PlayerId player) {

    final int nextRound = currentRound + 1;
    final int nextPos = currentPos / 2;
    final var nextBout = this.findBout(nextRound, nextPos);

    if (currentPos % 2 == 0) {
      nextBout.assignPlayerOne(player);
    } else {
      nextBout.assignPlayerTwo(player);
    }

    if (nextBout.hasBothPlayers()) {
      if (this.forfeitedPlayers.contains(nextBout.playerOne())) {
        nextBout.resolve(nextBout.playerTwo());
        advancePlayerToNextRound(nextRound, nextPos, nextBout.playerTwo());
      } else if (this.forfeitedPlayers.contains(nextBout.playerTwo())) {
        nextBout.resolve(nextBout.playerOne());
        advancePlayerToNextRound(nextRound, nextPos, nextBout.playerOne());
      } else {
        nextBout.transitionToPending();
      }
    }
  }

  public void linkBoutMatch(final BoutId boutId, final MatchId matchId) {

    Objects.requireNonNull(boutId, "BoutId cannot be null");
    Objects.requireNonNull(matchId, "MatchId cannot be null");

    final var bout = this.bouts.stream().filter(b -> b.id().equals(boutId)).findFirst()
        .orElseThrow(() -> new MatchNotPartOfCupException(matchId));

    if (matchId.equals(bout.matchId())) {
      return;
    }

    bout.linkMatch(matchId);
    LOGGER.info("Bout linked: cupId={}, boutId={}, matchId={}", this.id, boutId, matchId);
  }

  public MatchAdvancementResult recordMatchWinner(final MatchId matchId, final PlayerId winner) {

    Objects.requireNonNull(matchId, "MatchId cannot be null");
    Objects.requireNonNull(winner, "Winner cannot be null");

    final var bout = this.bouts.stream()
        .filter(b -> b.matchId() != null && b.matchId().equals(matchId)).findFirst()
        .orElseThrow(() -> new MatchNotPartOfCupException(matchId));

    if (bout.status() == BoutStatus.FINISHED) {
      if (winner.equals(bout.winner())) {
        return MatchAdvancementResult.empty();
      }
      throw new BoutAlreadyResolvedException();
    }

    if (!bout.containsPlayer(winner)) {
      throw new WinnerNotInBoutException();
    }

    final var pairings = new ArrayList<BoutPairing>();
    bout.resolve(winner);
    LOGGER.info("Cup result registered: cupId={}, matchId={}, winner={}", this.id, matchId, winner);

    if (this.isFinalRound(bout)) {
      this.champion = winner;
      this.status = CupStatus.FINISHED;
      LOGGER.info("Cup finished: cupId={}, champion={}", this.id, winner);
    } else {
      this.advanceWinnerToNextRound(bout.roundNumber(), bout.bracketPosition(), winner, pairings);
    }

    return new MatchAdvancementResult(List.copyOf(pairings), this.status == CupStatus.FINISHED,
        this.champion);
  }

  public MatchAdvancementResult forfeitPlayer(final PlayerId forfeiter) {

    Objects.requireNonNull(forfeiter, "Forfeiter cannot be null");

    if (!this.participants.contains(forfeiter)) {
      throw new PlayerNotInCupException();
    }

    this.forfeitedPlayers.add(forfeiter);

    final var activeBout = this.bouts.stream().filter(
        b -> (b.status() == BoutStatus.PENDING || b.status() == BoutStatus.AWAITING)
            && b.containsPlayer(forfeiter)).findFirst();

    final var pairings = new ArrayList<BoutPairing>();

    activeBout.ifPresent(bout -> {
      if (bout.status() == BoutStatus.PENDING) {
        final var opponent =
            bout.playerOne().equals(forfeiter) ? bout.playerTwo() : bout.playerOne();
        bout.resolve(opponent);
        LOGGER.info("Cup bout auto-resolved by forfeit: cupId={}, boutId={}, winner={}", this.id,
            bout.id(), opponent);
        if (this.isFinalRound(bout)) {
          this.champion = opponent;
          this.status = CupStatus.FINISHED;
          LOGGER.info("Cup finished by forfeit: cupId={}, champion={}", this.id, opponent);
        } else {
          this.advanceWinnerToNextRound(bout.roundNumber(), bout.bracketPosition(), opponent,
              pairings);
        }
      }
    });

    return new MatchAdvancementResult(List.copyOf(pairings), this.status == CupStatus.FINISHED,
        this.champion);
  }

  private void advanceWinnerToNextRound(final int currentRound, final int currentPos,
      final PlayerId winner, final List<BoutPairing> pairings) {

    final int nextRound = currentRound + 1;
    final int nextPos = currentPos / 2;
    final var nextBout = this.findBout(nextRound, nextPos);

    if (currentPos % 2 == 0) {
      nextBout.assignPlayerOne(winner);
    } else {
      nextBout.assignPlayerTwo(winner);
    }

    if (nextBout.hasBothPlayers()) {
      if (this.forfeitedPlayers.contains(nextBout.playerOne())) {
        nextBout.resolve(nextBout.playerTwo());
        if (this.isFinalRound(nextBout)) {
          this.champion = nextBout.playerTwo();
          this.status = CupStatus.FINISHED;
        } else {
          this.advanceWinnerToNextRound(nextRound, nextPos, nextBout.playerTwo(), pairings);
        }
      } else if (this.forfeitedPlayers.contains(nextBout.playerTwo())) {
        nextBout.resolve(nextBout.playerOne());
        if (this.isFinalRound(nextBout)) {
          this.champion = nextBout.playerOne();
          this.status = CupStatus.FINISHED;
        } else {
          this.advanceWinnerToNextRound(nextRound, nextPos, nextBout.playerOne(), pairings);
        }
      } else {
        nextBout.transitionToPending();
        pairings.add(new BoutPairing(nextBout.id(), nextBout.playerOne(), nextBout.playerTwo()));
      }
    }
  }

  private boolean isFinalRound(final Bout bout) {

    final int totalRounds = Integer.numberOfTrailingZeros(nextPowerOfTwo(this.participants.size()));
    return bout.roundNumber() == totalRounds;
  }

  private Bout findBout(final int roundNumber, final int bracketPosition) {

    return this.bouts.stream()
        .filter(b -> b.roundNumber() == roundNumber && b.bracketPosition() == bracketPosition)
        .findFirst().orElseThrow(() -> new BracketCorruptedException(roundNumber, bracketPosition));
  }

  public boolean hasPlayer(final PlayerId playerId) {

    return this.participants.contains(playerId);
  }

  public boolean isPlayerStillCompeting(final PlayerId playerId) {

    if (this.forfeitedPlayers.contains(playerId)) {
      return false;
    }
    final var eliminated = this.bouts.stream().anyMatch(
        b -> b.status() == BoutStatus.FINISHED && b.containsPlayer(playerId) && !playerId.equals(
            b.winner()));
    return !eliminated;
  }

  public CupStatus getStatus() {

    return this.status;
  }

  public InviteCode getInviteCode() {

    return this.inviteCode;
  }

  public GamesToPlay getGamesToPlay() {

    return this.gamesToPlay;
  }

  public PlayerId getChampion() {

    return this.champion;
  }

  public List<BoutView> getBouts() {

    return this.bouts.stream().map(Bout::toView).toList();
  }

  public List<RoundView> getRounds() {

    final var rounds = new java.util.LinkedHashMap<Integer, List<BoutView>>();
    for (final var bout : this.bouts) {
      rounds.computeIfAbsent(bout.roundNumber(), ignored -> new ArrayList<>()).add(bout.toView());
    }
    return rounds.entrySet().stream().map(entry -> new RoundView(entry.getKey(), entry.getValue()))
        .toList();
  }

  List<PlayerId> getParticipants() {

    return this.participants;
  }

  int getNumberOfPlayers() {

    return this.numberOfPlayers;
  }

  List<Bout> getBoutsInternal() {

    return this.bouts;
  }

  Set<PlayerId> getForfeitedPlayersInternal() {

    return this.forfeitedPlayers;
  }

  public record BoutView(BoutId boutId, int roundNumber, int bracketPosition, PlayerId playerOne,
                         PlayerId playerTwo, MatchId matchId, PlayerId winner, BoutStatus status) {

  }

  public record RoundView(int roundNumber, List<BoutView> bouts) {

  }

  static final class Bout {

    private final BoutId id;
    private final int roundNumber;
    private final int bracketPosition;
    private PlayerId playerOne;
    private PlayerId playerTwo;
    private MatchId matchId;
    private PlayerId winner;
    private BoutStatus status;

    private Bout(final BoutId id, final int roundNumber, final int bracketPosition,
        final BoutStatus status) {

      this.id = id;
      this.roundNumber = roundNumber;
      this.bracketPosition = bracketPosition;
      this.status = status;
    }

    static Bout awaiting(final BoutId id, final int roundNumber, final int bracketPosition) {

      return new Bout(id, roundNumber, bracketPosition, BoutStatus.AWAITING);
    }

    static Bout reconstruct(final BoutId id, final int roundNumber, final int bracketPosition,
        final PlayerId playerOne, final PlayerId playerTwo, final MatchId matchId,
        final PlayerId winner, final BoutStatus status) {

      final var bout = new Bout(id, roundNumber, bracketPosition, status);
      bout.playerOne = playerOne;
      bout.playerTwo = playerTwo;
      bout.matchId = matchId;
      bout.winner = winner;
      return bout;
    }

    void assignPlayerOne(final PlayerId player) {

      this.playerOne = player;
    }

    void assignPlayerTwo(final PlayerId player) {

      this.playerTwo = player;
    }

    boolean hasBothPlayers() {

      return this.playerOne != null && this.playerTwo != null;
    }

    void transitionToPending() {

      this.status = BoutStatus.PENDING;
    }

    void resolveAsBye(final PlayerId player) {

      this.playerOne = player;
      this.winner = player;
      this.status = BoutStatus.BYE;
    }

    void resolve(final PlayerId winner) {

      this.winner = winner;
      this.status = BoutStatus.FINISHED;
    }

    void linkMatch(final MatchId matchId) {

      this.matchId = matchId;
    }

    boolean containsPlayer(final PlayerId playerId) {

      return playerId.equals(this.playerOne) || (playerId.equals(this.playerTwo));
    }

    BoutId id() {

      return this.id;
    }

    int roundNumber() {

      return this.roundNumber;
    }

    int bracketPosition() {

      return this.bracketPosition;
    }

    PlayerId playerOne() {

      return this.playerOne;
    }

    PlayerId playerTwo() {

      return this.playerTwo;
    }

    MatchId matchId() {

      return this.matchId;
    }

    PlayerId winner() {

      return this.winner;
    }

    BoutStatus status() {

      return this.status;
    }

    BoutView toView() {

      return new BoutView(this.id, this.roundNumber, this.bracketPosition, this.playerOne,
          this.playerTwo, this.matchId, this.winner, this.status);
    }

  }

}
