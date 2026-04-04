package com.villo.truco.domain.model.league;

import com.villo.truco.domain.model.league.events.LeagueAdvancedEvent;
import com.villo.truco.domain.model.league.events.LeagueCancelledEvent;
import com.villo.truco.domain.model.league.events.LeagueDomainEvent;
import com.villo.truco.domain.model.league.events.LeagueFinishedEvent;
import com.villo.truco.domain.model.league.events.LeagueFixtureActivatedEvent;
import com.villo.truco.domain.model.league.events.LeagueMatchActivatedEvent;
import com.villo.truco.domain.model.league.events.LeaguePlayerForfeitedEvent;
import com.villo.truco.domain.model.league.events.LeaguePlayerJoinedEvent;
import com.villo.truco.domain.model.league.events.LeaguePlayerLeftEvent;
import com.villo.truco.domain.model.league.events.LeagueStartedEvent;
import com.villo.truco.domain.model.league.events.PublicLeagueLobbyOpenedEvent;
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
import com.villo.truco.domain.model.league.exceptions.PrivateLeagueVisibilityAccessException;
import com.villo.truco.domain.model.league.exceptions.PublicLeagueLobbyUnavailableException;
import com.villo.truco.domain.model.league.exceptions.WinnerNotInFixtureException;
import com.villo.truco.domain.model.league.valueobjects.FixtureActivation;
import com.villo.truco.domain.model.league.valueobjects.FixtureId;
import com.villo.truco.domain.model.league.valueobjects.FixtureStatus;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.league.valueobjects.LeagueStatus;
import com.villo.truco.domain.shared.AggregateBase;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
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
  private final Visibility visibility;
  private final InviteCode inviteCode;
  private LeagueStatus status;

  private League(final LeagueId id, final List<PlayerId> participants, final List<Fixture> fixtures,
      final Map<PlayerId, Integer> winsByPlayer, final LeagueStatus status,
      final int numberOfPlayers, final GamesToPlay gamesToPlay, final Visibility visibility,
      final InviteCode inviteCode) {

    super(id);
    this.participants = participants;
    this.fixtures = fixtures;
    this.winsByPlayer = winsByPlayer;
    this.status = status;
    this.numberOfPlayers = numberOfPlayers;
    this.gamesToPlay = gamesToPlay;
    this.visibility = visibility;
    this.inviteCode = inviteCode;
  }

  public static League create(final PlayerId creatorId, final int numberOfPlayers,
      final GamesToPlay gamesToPlay, final Visibility visibility) {

    Objects.requireNonNull(creatorId, "Creator cannot be null");
    Objects.requireNonNull(gamesToPlay, "GamesToPlay cannot be null");
    Objects.requireNonNull(visibility, "Visibility cannot be null");

    if (numberOfPlayers < 3 || numberOfPlayers > 8) {
      throw new InvalidLeaguePlayersException("League numberOfPlayers must be between 3 and 8");
    }

    final var participants = new ArrayList<PlayerId>();
    participants.add(creatorId);

    final var league = new League(LeagueId.generate(), participants, new ArrayList<>(),
        new LinkedHashMap<>(), LeagueStatus.WAITING_FOR_PLAYERS, numberOfPlayers, gamesToPlay,
        visibility, visibility == Visibility.PRIVATE ? InviteCode.generate() : null);
    if (visibility == Visibility.PUBLIC) {
      league.addDomainEvent(new PublicLeagueLobbyOpenedEvent(league.getId(), creatorId));
    }
    LOGGER.info(
        "League created: leagueId={}, creator={}, visibility={}, numberOfPlayers={}, gamesToPlay={}",
        league.getId(), creatorId, visibility, numberOfPlayers, gamesToPlay);
    return league;
  }

  static League reconstruct(final LeagueId id, final List<PlayerId> participants,
      final List<Fixture> fixtures, final Map<PlayerId, Integer> winsByPlayer,
      final LeagueStatus status, final int numberOfPlayers, final GamesToPlay gamesToPlay,
      final Visibility visibility, final InviteCode inviteCode) {

    return new League(id, participants, fixtures, winsByPlayer, status, numberOfPlayers,
        gamesToPlay, visibility, inviteCode);
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
          fixtures.add(Fixture.scheduled(FixtureId.generate(), matchday, playerOne, playerTwo));
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

  public List<FixtureActivation> start(final PlayerId playerId) {

    Objects.requireNonNull(playerId, "PlayerId cannot be null");

    if (this.status != LeagueStatus.WAITING_FOR_START) {
      throw new LeagueNotReadyException();
    }

    if (!this.participants.getFirst().equals(playerId)) {
      throw new OnlyCreatorCanStartException();
    }

    return this.startInternal();
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
    this.addDomainEvent(
        new LeaguePlayerJoinedEvent(this.id, List.copyOf(this.participants), playerId));

    if (this.participants.size() == this.numberOfPlayers) {
      this.status = LeagueStatus.WAITING_FOR_START;
      LOGGER.info("League ready: leagueId={}, participants={}", this.id, this.participants.size());
    }
  }

  public List<FixtureActivation> joinPublic(final PlayerId playerId) {

    Objects.requireNonNull(playerId, "PlayerId cannot be null");

    if (this.visibility != Visibility.PUBLIC) {
      throw new PrivateLeagueVisibilityAccessException();
    }

    if (this.status != LeagueStatus.WAITING_FOR_PLAYERS) {
      throw new PublicLeagueLobbyUnavailableException();
    }

    if (this.participants.contains(playerId)) {
      throw new PlayerAlreadyInLeagueException();
    }

    if (this.participants.size() >= this.numberOfPlayers) {
      throw new LeagueFullException();
    }

    this.participants.add(playerId);
    LOGGER.info("Player joined public league: leagueId={}, playerId={}, participants={}/{}",
        this.id, playerId, this.participants.size(), this.numberOfPlayers);
    this.addDomainEvent(
        new LeaguePlayerJoinedEvent(this.id, List.copyOf(this.participants), playerId));

    if (this.participants.size() != this.numberOfPlayers) {
      return List.of();
    }

    this.status = LeagueStatus.WAITING_FOR_START;
    LOGGER.info("Public league ready: leagueId={}, participants={}", this.id,
        this.participants.size());

    return this.startInternal();
  }

  private void initializeFixtures() {

    this.fixtures.addAll(generateRoundRobinFixtures(this.participants));

    for (final var participant : this.participants) {
      this.winsByPlayer.put(participant, 0);
    }

    this.status = LeagueStatus.IN_PROGRESS;
    LOGGER.info("League started: leagueId={}, participants={}, fixtures={}", this.id,
        this.participants.size(), this.fixtures.size());
    this.addDomainEvent(new LeagueStartedEvent(this.id, List.copyOf(this.participants)));
  }

  private List<FixtureActivation> startInternal() {

    this.initializeFixtures();
    return this.activateNextFixtures();
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
    this.addDomainEvent(
        new LeagueMatchActivatedEvent(this.id, List.copyOf(this.participants), matchId));
    LOGGER.info("Fixture linked: leagueId={}, fixtureId={}, matchId={}", this.id, fixtureId,
        matchId);
  }

  List<FixtureActivation> activateNextFixtures() {

    final var activations = new ArrayList<FixtureActivation>();

    for (final var fixture : this.fixtures) {
      if (fixture.status() != FixtureStatus.SCHEDULED) {
        continue;
      }
      if (canActivate(fixture)) {
        fixture.activate();
        activations.add(
            new FixtureActivation(fixture.id(), fixture.playerOne(), fixture.playerTwo()));
        LOGGER.info("Fixture activated: leagueId={}, fixtureId={}, matchday={}", this.id,
            fixture.id(), fixture.matchdayNumber());
        this.addDomainEvent(
            new LeagueFixtureActivatedEvent(this.id, List.copyOf(this.participants), fixture.id()));
      }
    }

    return activations;
  }

  private boolean canActivate(final Fixture fixture) {

    return isPlayerReadyForMatchday(fixture.playerOne(), fixture.matchdayNumber())
        && isPlayerReadyForMatchday(fixture.playerTwo(), fixture.matchdayNumber());
  }

  private boolean isPlayerReadyForMatchday(final PlayerId player, final int matchdayNumber) {

    final var allEarlierResolved = this.fixtures.stream()
        .filter(f -> f.containsPlayer(player) && f.matchdayNumber() < matchdayNumber)
        .allMatch(f -> f.status() == FixtureStatus.FINISHED || f.status() == FixtureStatus.LIBRE);

    final var noPendingFixture = this.fixtures.stream()
        .noneMatch(f -> f.containsPlayer(player) && f.status() == FixtureStatus.PENDING);

    return allEarlierResolved && noPendingFixture;
  }

  public List<FixtureActivation> forfeitPlayer(final PlayerId forfeiter) {

    Objects.requireNonNull(forfeiter, "Forfeiter cannot be null");

    this.validatePlayerInLeague(forfeiter);

    final var unresolvedFixtures = this.fixtures.stream().filter(
        f -> (f.status() == FixtureStatus.PENDING || f.status() == FixtureStatus.SCHEDULED)
            && f.containsPlayer(forfeiter)).toList();

    for (final var fixture : unresolvedFixtures) {
      final var rival =
          fixture.playerOne().equals(forfeiter) ? fixture.playerTwo() : fixture.playerOne();
      fixture.resolve(rival);
      this.winsByPlayer.merge(rival, 1, Integer::sum);
      LOGGER.info("Fixture auto-resolved by forfeit: leagueId={}, fixtureId={}, winner={}", this.id,
          fixture.id(), rival);
      this.addDomainEvent(
          new LeagueAdvancedEvent(this.id, List.copyOf(this.participants), fixture.matchId(),
              rival));
    }

    this.addDomainEvent(
        new LeaguePlayerForfeitedEvent(this.id, List.copyOf(this.participants), forfeiter));

    final var allResolved = this.fixtures.stream()
        .allMatch(f -> f.status() == FixtureStatus.FINISHED || f.status() == FixtureStatus.LIBRE);

    if (allResolved) {
      this.status = LeagueStatus.FINISHED;
      LOGGER.info("League finished by forfeit: leagueId={}, leaders={}", this.id,
          this.getLeaders());
      this.addDomainEvent(
          new LeagueFinishedEvent(this.id, List.copyOf(this.participants), this.getLeaders()));
    }

    return this.activateNextFixtures();
  }

  public List<FixtureActivation> recordMatchWinner(final MatchId matchId, final PlayerId winner) {

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
    this.addDomainEvent(
        new LeagueAdvancedEvent(this.id, List.copyOf(this.participants), matchId, winner));

    final var allResolved = this.fixtures.stream().allMatch(
        it -> it.status() == FixtureStatus.FINISHED || it.status() == FixtureStatus.LIBRE);

    if (allResolved) {
      this.status = LeagueStatus.FINISHED;
      LOGGER.info("League finished: leagueId={}, leaders={}", this.id, this.getLeaders());
      this.addDomainEvent(
          new LeagueFinishedEvent(this.id, List.copyOf(this.participants), this.getLeaders()));
    }

    return this.activateNextFixtures();
  }

  public boolean hasPlayer(final PlayerId playerId) {

    return this.participants.contains(playerId);
  }

  public void validatePlayerInLeague(final PlayerId playerId) {

    if (!this.participants.contains(playerId)) {
      throw new PlayerNotInLeagueException();
    }
  }

  public boolean hasPlayerPendingFixtures(final PlayerId playerId) {

    return this.fixtures.stream().anyMatch(
        f -> (f.status() == FixtureStatus.PENDING || f.status() == FixtureStatus.SCHEDULED)
            && f.containsPlayer(playerId));
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

  public Visibility getVisibility() {

    return this.visibility;
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

  public void cancel() {

    if (this.status == LeagueStatus.WAITING_FOR_PLAYERS
        || this.status == LeagueStatus.WAITING_FOR_START) {
      final var participantsCopy = List.copyOf(this.participants);
      this.participants.clear();
      this.status = LeagueStatus.CANCELLED;
      LOGGER.info("League cancelled by timeout: leagueId={}", this.id);
      this.addDomainEvent(new LeagueCancelledEvent(this.id, participantsCopy));
    }
  }

  public void leave(final PlayerId playerId) {

    Objects.requireNonNull(playerId, "PlayerId cannot be null");

    if (this.status != LeagueStatus.WAITING_FOR_PLAYERS
        && this.status != LeagueStatus.WAITING_FOR_START) {
      throw new LeagueNotWaitingException();
    }

    this.validatePlayerInLeague(playerId);

    if (this.participants.getFirst().equals(playerId)) {
      final var participantsCopy = List.copyOf(this.participants);
      this.participants.clear();
      this.status = LeagueStatus.CANCELLED;
      LOGGER.info("League cancelled by creator: leagueId={}, creatorId={}", this.id, playerId);
      this.addDomainEvent(new LeagueCancelledEvent(this.id, participantsCopy));
      return;
    }

    this.participants.remove(playerId);
    this.status = LeagueStatus.WAITING_FOR_PLAYERS;
    LOGGER.info("Player left league: leagueId={}, playerId={}, participants={}/{}", this.id,
        playerId, this.participants.size(), this.numberOfPlayers);
    this.addDomainEvent(
        new LeaguePlayerLeftEvent(this.id, List.copyOf(this.participants), playerId));
  }

  public List<PlayerId> getParticipants() {

    return this.participants;
  }

  public boolean isPublicLobbyOpen() {

    return this.visibility == Visibility.PUBLIC && this.status == LeagueStatus.WAITING_FOR_PLAYERS;
  }

  public PlayerId getCreator() {

    return this.participants.getFirst();
  }

  public int getNumberOfPlayers() {

    return this.numberOfPlayers;
  }

  List<Fixture> getFixturesInternal() {

    return this.fixtures;
  }

  public List<LeagueDomainEvent> getLeagueDomainEvents() {

    return getDomainEvents().stream().map(LeagueDomainEvent.class::cast).toList();
  }

}
