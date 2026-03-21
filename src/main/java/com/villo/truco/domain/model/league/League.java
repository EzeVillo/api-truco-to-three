package com.villo.truco.domain.model.league;

import com.villo.truco.domain.model.league.exceptions.FixtureAlreadyResolvedException;
import com.villo.truco.domain.model.league.exceptions.InvalidLeagueInviteCodeException;
import com.villo.truco.domain.model.league.exceptions.InvalidLeaguePlayersException;
import com.villo.truco.domain.model.league.exceptions.LeagueFullException;
import com.villo.truco.domain.model.league.exceptions.LeagueNotReadyException;
import com.villo.truco.domain.model.league.exceptions.LeagueNotWaitingException;
import com.villo.truco.domain.model.league.exceptions.MatchNotPartOfLeagueException;
import com.villo.truco.domain.model.league.exceptions.OnlyCreatorCanStartException;
import com.villo.truco.domain.model.league.exceptions.PlayerAlreadyInLeagueException;
import com.villo.truco.domain.model.league.exceptions.PlayerNotInLeagueException;
import com.villo.truco.domain.model.league.exceptions.WinnerNotInFixtureException;
import com.villo.truco.domain.model.league.valueobjects.FixtureId;
import com.villo.truco.domain.model.league.valueobjects.FixtureStatus;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.league.valueobjects.LeagueStatus;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
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

public final class League extends AggregateBase<LeagueId> {

  private static final Logger LOGGER = LoggerFactory.getLogger(League.class);

  private final List<PlayerId> participants;
  private final List<Fixture> fixtures;
  private final Map<PlayerId, Integer> winsByPlayer;
  private final int numberOfPlayers;
  private final GamesToPlay gamesToPlay;
  private final InviteCode inviteCode;
  private LeagueStatus status;

  private League(final LeagueId id, final List<PlayerId> participants, final List<Fixture> fixtures,
      final Map<PlayerId, Integer> winsByPlayer, final LeagueStatus status,
      final int numberOfPlayers, final GamesToPlay gamesToPlay, final InviteCode inviteCode) {

    super(id);
    this.participants = participants;
    this.fixtures = fixtures;
    this.winsByPlayer = winsByPlayer;
    this.status = status;
    this.numberOfPlayers = numberOfPlayers;
    this.gamesToPlay = gamesToPlay;
    this.inviteCode = inviteCode;
  }

  public static League create(final PlayerId creatorId, final int numberOfPlayers,
      final GamesToPlay gamesToPlay) {

    Objects.requireNonNull(creatorId, "Creator cannot be null");
    Objects.requireNonNull(gamesToPlay, "GamesToPlay cannot be null");

    if (numberOfPlayers < 3 || numberOfPlayers > 8) {
      throw new InvalidLeaguePlayersException("League numberOfPlayers must be between 3 and 8");
    }

    final var participants = new ArrayList<PlayerId>();
    participants.add(creatorId);

    final var league = new League(LeagueId.generate(), participants, new ArrayList<>(),
        new LinkedHashMap<>(), LeagueStatus.WAITING_FOR_PLAYERS, numberOfPlayers, gamesToPlay,
        InviteCode.generate());
    LOGGER.info("League created: leagueId={}, creator={}, numberOfPlayers={}, gamesToPlay={}",
        league.getId(), creatorId, numberOfPlayers, gamesToPlay);
    return league;
  }

  static League reconstruct(final LeagueId id, final List<PlayerId> participants,
      final List<Fixture> fixtures, final Map<PlayerId, Integer> winsByPlayer,
      final LeagueStatus status, final int numberOfPlayers, final GamesToPlay gamesToPlay,
      final InviteCode inviteCode) {

    return new League(id, participants, fixtures, winsByPlayer, status, numberOfPlayers,
        gamesToPlay, inviteCode);
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
        final var home = rotation.get(matchIndex);
        final var away = rotation.get(teamCount - 1 - matchIndex);

        if (home == null || away == null) {
          final var freePlayer = home != null ? home : away;
          fixtures.add(Fixture.free(FixtureId.generate(), matchday, freePlayer));
        } else {
          final var shouldSwap = matchday % 2 == 0;
          final var playerOne = shouldSwap ? away : home;
          final var playerTwo = shouldSwap ? home : away;
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

  public void start(final PlayerId playerId) {

    Objects.requireNonNull(playerId, "PlayerId cannot be null");

    if (this.status != LeagueStatus.WAITING_FOR_START) {
      throw new LeagueNotReadyException();
    }

    if (!this.participants.getFirst().equals(playerId)) {
      throw new OnlyCreatorCanStartException();
    }

    this.initializeFixtures();
  }

  public void join(final PlayerId playerId, final InviteCode inviteCode) {

    Objects.requireNonNull(playerId, "PlayerId cannot be null");
    Objects.requireNonNull(inviteCode, "InviteCode cannot be null");

    if (this.status != LeagueStatus.WAITING_FOR_PLAYERS) {
      throw new LeagueNotWaitingException();
    }

    if (!this.inviteCode.equals(inviteCode)) {
      throw new InvalidLeagueInviteCodeException();
    }

    if (this.participants.contains(playerId)) {
      throw new PlayerAlreadyInLeagueException();
    }

    if (this.participants.size() >= this.numberOfPlayers) {
      throw new LeagueFullException();
    }

    this.participants.add(playerId);
    LOGGER.info("Player joined league: leagueId={}, playerId={}, participants={}/{}", this.id,
        playerId, this.participants.size(), this.numberOfPlayers);

    if (this.participants.size() == this.numberOfPlayers) {
      this.status = LeagueStatus.WAITING_FOR_START;
      LOGGER.info("League ready: leagueId={}, participants={}", this.id, this.participants.size());
    }
  }

  private void initializeFixtures() {

    this.fixtures.addAll(generateRoundRobinFixtures(this.participants));

    for (final var participant : this.participants) {
      this.winsByPlayer.put(participant, 0);
    }

    this.status = LeagueStatus.IN_PROGRESS;
    LOGGER.info("League started: leagueId={}, participants={}, fixtures={}", this.id,
        this.participants.size(), this.fixtures.size());
  }

  public void linkFixtureMatch(final FixtureId fixtureId, final MatchId matchId) {

    Objects.requireNonNull(fixtureId, "FixtureId cannot be null");
    Objects.requireNonNull(matchId, "MatchId cannot be null");

    final var fixture = this.fixtures.stream().filter(it -> it.id().equals(fixtureId)).findFirst()
        .orElseThrow(() -> new MatchNotPartOfLeagueException(matchId));

    if (fixture.status() == FixtureStatus.LIBRE) {
      LOGGER.debug("Skipping match link for LIBRE fixture: leagueId={}, fixtureId={}", this.id,
          fixtureId);
      return;
    }

    fixture.linkMatch(matchId);
    LOGGER.info("Fixture linked: leagueId={}, fixtureId={}, matchId={}", this.id, fixtureId,
        matchId);
  }

  public void forfeitPlayer(final PlayerId forfeiter) {

    Objects.requireNonNull(forfeiter, "Forfeiter cannot be null");

    if (!this.participants.contains(forfeiter)) {
      throw new PlayerNotInLeagueException();
    }

    final var pendingFixtures = this.fixtures.stream()
        .filter(f -> f.status() == FixtureStatus.PENDING && f.containsPlayer(forfeiter)).toList();

    for (final var fixture : pendingFixtures) {
      final var rival =
          fixture.playerOne().equals(forfeiter) ? fixture.playerTwo() : fixture.playerOne();
      fixture.resolve(rival);
      this.winsByPlayer.merge(rival, 1, Integer::sum);
      LOGGER.info("Fixture auto-resolved by forfeit: leagueId={}, fixtureId={}, winner={}", this.id,
          fixture.id(), rival);
    }

    final var allResolved = this.fixtures.stream()
        .allMatch(f -> f.status() != FixtureStatus.PENDING);

    if (allResolved) {
      this.status = LeagueStatus.FINISHED;
      LOGGER.info("League finished by forfeit: leagueId={}, leaders={}", this.id,
          this.getLeaders());
    }
  }

  public void recordMatchWinner(final MatchId matchId, final PlayerId winner) {

    Objects.requireNonNull(matchId, "MatchId cannot be null");
    Objects.requireNonNull(winner, "Winner cannot be null");

    final var fixture = this.fixtures.stream()
        .filter(it -> it.matchId() != null && it.matchId().equals(matchId)).findFirst()
        .orElseThrow(() -> new MatchNotPartOfLeagueException(matchId));

    if (fixture.status() == FixtureStatus.FINISHED) {
      throw new FixtureAlreadyResolvedException();
    }

    if (!fixture.containsPlayer(winner)) {
      throw new WinnerNotInFixtureException();
    }

    fixture.resolve(winner);
    this.winsByPlayer.merge(winner, 1, Integer::sum);
    LOGGER.info("League result registered: leagueId={}, matchId={}, winner={}", this.id, matchId,
        winner);

    final var allResolved = this.fixtures.stream()
        .allMatch(it -> it.status() != FixtureStatus.PENDING);

    if (allResolved) {
      this.status = LeagueStatus.FINISHED;
      LOGGER.info("League finished: leagueId={}, leaders={}", this.id, this.getLeaders());
    }
  }

  public boolean hasPlayer(final PlayerId playerId) {

    return this.participants.contains(playerId);
  }

  public boolean hasPlayerPendingFixtures(final PlayerId playerId) {

    return this.fixtures.stream()
        .anyMatch(f -> f.status() == FixtureStatus.PENDING && f.containsPlayer(playerId));
  }

  public LeagueStatus getStatus() {

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

  public void leave(final PlayerId playerId) {

    Objects.requireNonNull(playerId, "PlayerId cannot be null");

    if (this.status != LeagueStatus.WAITING_FOR_PLAYERS
        && this.status != LeagueStatus.WAITING_FOR_START) {
      throw new LeagueNotWaitingException();
    }

    if (!this.participants.contains(playerId)) {
      throw new PlayerNotInLeagueException();
    }

    if (this.participants.getFirst().equals(playerId)) {
      this.participants.clear();
      this.status = LeagueStatus.CANCELLED;
      LOGGER.info("League cancelled by creator: leagueId={}, creatorId={}", this.id, playerId);
      return;
    }

    this.participants.remove(playerId);
    this.status = LeagueStatus.WAITING_FOR_PLAYERS;
    LOGGER.info("Player left league: leagueId={}, playerId={}, participants={}/{}", this.id,
        playerId, this.participants.size(), this.numberOfPlayers);
  }

  List<PlayerId> getParticipants() {

    return this.participants;
  }

  int getNumberOfPlayers() {

    return this.numberOfPlayers;
  }

  List<Fixture> getFixturesInternal() {

    return this.fixtures;
  }

  public record FixtureView(FixtureId fixtureId, int matchdayNumber, PlayerId playerOne,
                            PlayerId playerTwo, MatchId matchId, PlayerId winner,
                            FixtureStatus status) {

  }

  public record MatchdayView(int matchdayNumber, List<FixtureView> fixtures) {

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

    static Fixture pending(final FixtureId id, final int matchdayNumber, final PlayerId playerOne,
        final PlayerId playerTwo) {

      return new Fixture(id, matchdayNumber, playerOne, playerTwo, FixtureStatus.PENDING);
    }

    static Fixture free(final FixtureId id, final int matchdayNumber, final PlayerId freePlayer) {

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
