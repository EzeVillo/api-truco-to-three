package com.villo.truco.domain.model.tournament;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.tournament.exceptions.FixtureAlreadyResolvedException;
import com.villo.truco.domain.model.tournament.exceptions.InvalidTournamentInviteCodeException;
import com.villo.truco.domain.model.tournament.exceptions.InvalidTournamentPlayersException;
import com.villo.truco.domain.model.tournament.exceptions.MatchNotPartOfTournamentException;
import com.villo.truco.domain.model.tournament.exceptions.OnlyCreatorCanStartException;
import com.villo.truco.domain.model.tournament.exceptions.PlayerAlreadyInTournamentException;
import com.villo.truco.domain.model.tournament.exceptions.PlayerNotInTournamentException;
import com.villo.truco.domain.model.tournament.exceptions.TournamentCreatorCannotLeaveException;
import com.villo.truco.domain.model.tournament.exceptions.TournamentFullException;
import com.villo.truco.domain.model.tournament.exceptions.TournamentNotReadyException;
import com.villo.truco.domain.model.tournament.exceptions.TournamentNotWaitingException;
import com.villo.truco.domain.model.tournament.exceptions.WinnerNotInFixtureException;
import com.villo.truco.domain.model.tournament.valueobjects.FixtureId;
import com.villo.truco.domain.model.tournament.valueobjects.FixtureStatus;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentStatus;
import com.villo.truco.domain.shared.AggregateBase;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Tournament extends AggregateBase<TournamentId> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Tournament.class);

  private final List<PlayerId> participants;
  private final List<Fixture> fixtures;
  private final Map<PlayerId, Integer> winsByPlayer;
  private final int capacity;
  private final GamesToPlay gamesToPlay;
  private final InviteCode inviteCode;
  private TournamentStatus status;

  private Tournament(final TournamentId id, final List<PlayerId> participants,
      final List<Fixture> fixtures, final Map<PlayerId, Integer> winsByPlayer,
      final TournamentStatus status, final int capacity, final GamesToPlay gamesToPlay,
      final InviteCode inviteCode) {

    super(id);
    this.participants = participants;
    this.fixtures = fixtures;
    this.winsByPlayer = winsByPlayer;
    this.status = status;
    this.capacity = capacity;
    this.gamesToPlay = gamesToPlay;
    this.inviteCode = inviteCode;
  }

  public static Tournament create(final PlayerId creatorId, final int capacity,
      final GamesToPlay gamesToPlay) {

    Objects.requireNonNull(creatorId, "Creator cannot be null");
    Objects.requireNonNull(gamesToPlay, "GamesToPlay cannot be null");

    if (capacity < 3 || capacity > 8) {
      throw new InvalidTournamentPlayersException("Tournament capacity must be between 3 and 8");
    }

    final var participants = new ArrayList<PlayerId>();
    participants.add(creatorId);

    final var tournament = new Tournament(TournamentId.generate(), participants, new ArrayList<>(),
        new LinkedHashMap<>(), TournamentStatus.WAITING_FOR_PLAYERS, capacity, gamesToPlay,
        InviteCode.generate());
    LOGGER.info("Tournament created: tournamentId={}, creator={}, capacity={}, gamesToPlay={}",
        tournament.getId(), creatorId, capacity, gamesToPlay);
    return tournament;
  }

  public void join(final PlayerId playerId, final InviteCode inviteCode) {

    Objects.requireNonNull(playerId, "PlayerId cannot be null");
    Objects.requireNonNull(inviteCode, "InviteCode cannot be null");

    if (this.status != TournamentStatus.WAITING_FOR_PLAYERS) {
      throw new TournamentNotWaitingException();
    }

    if (!this.inviteCode.equals(inviteCode)) {
      throw new InvalidTournamentInviteCodeException();
    }

    if (this.participants.contains(playerId)) {
      throw new PlayerAlreadyInTournamentException();
    }

    if (this.participants.size() >= this.capacity) {
      throw new TournamentFullException();
    }

    this.participants.add(playerId);
    LOGGER.info("Player joined tournament: tournamentId={}, playerId={}, participants={}/{}",
        this.id, playerId, this.participants.size(), this.capacity);

    if (this.participants.size() == this.capacity) {
      this.status = TournamentStatus.WAITING_FOR_START;
      LOGGER.info("Tournament ready: tournamentId={}, participants={}", this.id,
          this.participants.size());
    }
  }

  public void start(final PlayerId playerId) {

    Objects.requireNonNull(playerId, "PlayerId cannot be null");

    if (this.status != TournamentStatus.WAITING_FOR_START) {
      throw new TournamentNotReadyException();
    }

    if (!this.participants.getFirst().equals(playerId)) {
      throw new OnlyCreatorCanStartException();
    }

    this.initializeFixtures();
  }

  public void leave(final PlayerId playerId) {

    Objects.requireNonNull(playerId, "PlayerId cannot be null");

    if (this.status != TournamentStatus.WAITING_FOR_PLAYERS
        && this.status != TournamentStatus.WAITING_FOR_START) {
      throw new TournamentNotWaitingException();
    }

    if (!this.participants.contains(playerId)) {
      throw new PlayerNotInTournamentException();
    }

    if (this.participants.getFirst().equals(playerId)) {
      throw new TournamentCreatorCannotLeaveException();
    }

    this.participants.remove(playerId);
    this.status = TournamentStatus.WAITING_FOR_PLAYERS;
    LOGGER.info("Player left tournament: tournamentId={}, playerId={}, participants={}/{}", this.id,
        playerId, this.participants.size(), this.capacity);
  }

  private void initializeFixtures() {

    this.fixtures.addAll(generateRoundRobinFixtures(this.participants));

    for (final var participant : this.participants) {
      this.winsByPlayer.put(participant, 0);
    }

    this.status = TournamentStatus.IN_PROGRESS;
    LOGGER.info("Tournament started: tournamentId={}, participants={}, fixtures={}", this.id,
        this.participants.size(), this.fixtures.size());
  }

  private static List<Fixture> generateRoundRobinFixtures(final List<PlayerId> participants) {

    final var rotation = new ArrayList<>(participants);

    final var hasOddParticipants = rotation.size() % 2 != 0;

    if (hasOddParticipants) {
      rotation.add(null);
    }

    final var teamCount = rotation.size();
    final var matchdays = teamCount - 1;
    final var matchesPerMatchday = teamCount / 2;
    final var fixtures = new ArrayList<Fixture>();

    for (var matchday = 1; matchday <= matchdays; matchday++) {
      for (var matchIndex = 0; matchIndex < matchesPerMatchday; matchIndex++) {
        final var playerOne = rotation.get(matchIndex);
        final var playerTwo = rotation.get(teamCount - 1 - matchIndex);

        if (playerOne == null || playerTwo == null) {
          final var freePlayer = playerOne != null ? playerOne : playerTwo;
          fixtures.add(Fixture.free(FixtureId.generate(), matchday, freePlayer));
        } else {
          fixtures.add(Fixture.pending(FixtureId.generate(), matchday, playerOne, playerTwo));
        }
      }

      rotateKeepingFirstFixed(rotation);
    }

    return fixtures;
  }

  private static void rotateKeepingFirstFixed(final List<PlayerId> rotation) {

    final var last = rotation.removeLast();
    rotation.add(1, last);
  }

  public void linkFixtureMatch(final FixtureId fixtureId, final MatchId matchId) {

    Objects.requireNonNull(fixtureId, "FixtureId cannot be null");
    Objects.requireNonNull(matchId, "MatchId cannot be null");

    final var fixture = this.fixtures.stream().filter(it -> it.id().equals(fixtureId)).findFirst()
        .orElseThrow(() -> new MatchNotPartOfTournamentException(matchId));

    if (fixture.status() == FixtureStatus.LIBRE) {
      LOGGER.debug("Skipping match link for LIBRE fixture: tournamentId={}, fixtureId={}", this.id,
          fixtureId);
      return;
    }

    fixture.linkMatch(matchId);
    LOGGER.info("Fixture linked: tournamentId={}, fixtureId={}, matchId={}", this.id, fixtureId,
        matchId);
  }

  public void forfeitPlayer(final PlayerId forfeiter) {

    Objects.requireNonNull(forfeiter, "Forfeiter cannot be null");

    if (!this.participants.contains(forfeiter)) {
      throw new PlayerNotInTournamentException();
    }

    final var pendingFixtures = this.fixtures.stream()
        .filter(f -> f.status() == FixtureStatus.PENDING && f.containsPlayer(forfeiter)).toList();

    for (final var fixture : pendingFixtures) {
      final var rival =
          fixture.playerOne().equals(forfeiter) ? fixture.playerTwo() : fixture.playerOne();
      fixture.resolve(rival);
      this.winsByPlayer.merge(rival, 1, Integer::sum);
      LOGGER.info("Fixture auto-resolved by forfeit: tournamentId={}, fixtureId={}, winner={}",
          this.id, fixture.id(), rival);
    }

    final var allResolved = this.fixtures.stream()
        .allMatch(f -> f.status() != FixtureStatus.PENDING);

    if (allResolved) {
      this.status = TournamentStatus.FINISHED;
      LOGGER.info("Tournament finished by forfeit: tournamentId={}, leaders={}", this.id,
          this.getLeaders());
    }
  }

  public void recordMatchWinner(final MatchId matchId, final PlayerId winner) {

    Objects.requireNonNull(matchId, "MatchId cannot be null");
    Objects.requireNonNull(winner, "Winner cannot be null");

    final var fixture = this.fixtures.stream()
        .filter(it -> it.matchId() != null && it.matchId().equals(matchId)).findFirst()
        .orElseThrow(() -> new MatchNotPartOfTournamentException(matchId));

    if (fixture.status() == FixtureStatus.FINISHED) {
      throw new FixtureAlreadyResolvedException();
    }

    if (!fixture.containsPlayer(winner)) {
      throw new WinnerNotInFixtureException();
    }

    fixture.resolve(winner);
    this.winsByPlayer.merge(winner, 1, Integer::sum);
    LOGGER.info("Tournament result registered: tournamentId={}, matchId={}, winner={}", this.id,
        matchId, winner);

    final var allResolved = this.fixtures.stream()
        .allMatch(it -> it.status() != FixtureStatus.PENDING);

    if (allResolved) {
      this.status = TournamentStatus.FINISHED;
      LOGGER.info("Tournament finished: tournamentId={}, leaders={}", this.id, this.getLeaders());
    }
  }

  public boolean hasPlayer(final PlayerId playerId) {

    return this.participants.contains(playerId);
  }

  public TournamentStatus getStatus() {

    return this.status;
  }

  public InviteCode getInviteCode() {

    return this.inviteCode;
  }

  public GamesToPlay getGamesToPlay() {

    return this.gamesToPlay;
  }

  public List<FixtureView> getFixtures() {

    return this.fixtures.stream().map(Fixture::toView).toList();
  }

  public List<MatchdayView> getMatchdays() {

    final var byMatchday = new LinkedHashMap<Integer, List<FixtureView>>();

    for (final var fixture : this.fixtures) {
      byMatchday.computeIfAbsent(fixture.matchdayNumber(), ignored -> new ArrayList<>())
          .add(fixture.toView());
    }

    return byMatchday.entrySet().stream()
        .map(entry -> new MatchdayView(entry.getKey(), entry.getValue())).toList();
  }

  public Map<PlayerId, Integer> getWinsByPlayer() {

    return Map.copyOf(this.winsByPlayer);
  }

  public List<PlayerId> getLeaders() {

    if (this.winsByPlayer.isEmpty()) {
      return List.of();
    }

    final var maxWins = this.winsByPlayer.values().stream().mapToInt(Integer::intValue).max()
        .orElse(0);

    return this.winsByPlayer.entrySet().stream().filter(entry -> entry.getValue() == maxWins)
        .map(Map.Entry::getKey).toList();
  }

  public record FixtureView(FixtureId fixtureId, int matchdayNumber, PlayerId playerOne,
                            PlayerId playerTwo, MatchId matchId, PlayerId winner,
                            FixtureStatus status) {

  }

  public record MatchdayView(int matchdayNumber, List<FixtureView> fixtures) {

  }

  static Tournament reconstruct(final TournamentId id, final List<PlayerId> participants,
      final List<Fixture> fixtures, final Map<PlayerId, Integer> winsByPlayer,
      final TournamentStatus status, final int capacity, final GamesToPlay gamesToPlay,
      final InviteCode inviteCode) {

    return new Tournament(id, participants, fixtures, winsByPlayer, status, capacity, gamesToPlay,
        inviteCode);
  }

  List<PlayerId> getParticipants() {

    return this.participants;
  }

  int getCapacity() {

    return this.capacity;
  }

  List<Fixture> getFixturesInternal() {

    return this.fixtures;
  }

  static final class Fixture {

    private final FixtureId id;
    private final int matchdayNumber;
    private final PlayerId playerOne;
    private final PlayerId playerTwo;
    private MatchId matchId;
    private PlayerId winner;
    private FixtureStatus status;

    private Fixture(final FixtureId id, final int matchdayNumber, final PlayerId playerOne,
        final PlayerId playerTwo, final FixtureStatus status) {

      this.id = id;
      this.matchdayNumber = matchdayNumber;
      this.playerOne = playerOne;
      this.playerTwo = playerTwo;
      this.status = status;
    }

    static Fixture pending(final FixtureId id, final int matchdayNumber,
        final PlayerId playerOne, final PlayerId playerTwo) {

      return new Fixture(id, matchdayNumber, playerOne, playerTwo, FixtureStatus.PENDING);
    }

    static Fixture free(final FixtureId id, final int matchdayNumber,
        final PlayerId freePlayer) {

      return new Fixture(id, matchdayNumber, freePlayer, null, FixtureStatus.LIBRE);
    }

    static Fixture reconstruct(final FixtureId id, final int matchdayNumber,
        final PlayerId playerOne, final PlayerId playerTwo, final MatchId matchId,
        final PlayerId winner, final FixtureStatus status) {

      final var fixture = new Fixture(id, matchdayNumber, playerOne, playerTwo, status);
      fixture.matchId = matchId;
      fixture.winner = winner;
      return fixture;
    }

    FixtureId id() {

      return this.id;
    }

    MatchId matchId() {

      return this.matchId;
    }

    FixtureStatus status() {

      return this.status;
    }

    int matchdayNumber() {

      return this.matchdayNumber;
    }

    PlayerId playerOne() {

      return this.playerOne;
    }

    PlayerId playerTwo() {

      return this.playerTwo;
    }

    private boolean containsPlayer(final PlayerId playerId) {

      final var playerTwoMatches = this.playerTwo != null && this.playerTwo.equals(playerId);
      return this.playerOne.equals(playerId) || playerTwoMatches;
    }

    private void linkMatch(final MatchId matchId) {

      this.matchId = matchId;
    }

    private void resolve(final PlayerId winner) {

      this.winner = winner;
      this.status = FixtureStatus.FINISHED;
    }

    PlayerId winner() {

      return this.winner;
    }

    FixtureView toView() {

      return new FixtureView(this.id, this.matchdayNumber, this.playerOne, this.playerTwo,
          this.matchId, this.winner, this.status);
    }

  }

}
