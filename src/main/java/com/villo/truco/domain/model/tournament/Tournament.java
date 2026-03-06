package com.villo.truco.domain.model.tournament;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import com.villo.truco.domain.model.tournament.exceptions.FixtureAlreadyResolvedException;
import com.villo.truco.domain.model.tournament.exceptions.InvalidTournamentPlayersException;
import com.villo.truco.domain.model.tournament.exceptions.MatchNotPartOfTournamentException;
import com.villo.truco.domain.model.tournament.exceptions.WinnerNotInFixtureException;
import com.villo.truco.domain.model.tournament.valueobjects.FixtureId;
import com.villo.truco.domain.model.tournament.valueobjects.FixtureStatus;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentStatus;
import com.villo.truco.domain.shared.AggregateBase;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
  private TournamentStatus status;

  private Tournament(final TournamentId id, final List<PlayerId> participants,
      final List<Fixture> fixtures, final Map<PlayerId, Integer> winsByPlayer,
      final TournamentStatus status) {

    super(id);
    this.participants = participants;
    this.fixtures = fixtures;
    this.winsByPlayer = winsByPlayer;
    this.status = status;
  }

  public static Tournament create(final List<PlayerId> participantIds) {

    Objects.requireNonNull(participantIds, "Participants cannot be null");

    if (participantIds.size() < 2) {
      throw new InvalidTournamentPlayersException("Tournament requires at least 2 players");
    }

    final var unique = new LinkedHashSet<>(participantIds);

    if (unique.size() != participantIds.size()) {
      throw new InvalidTournamentPlayersException("Tournament participants must be unique");
    }

    final var participants = List.copyOf(participantIds);
    final var fixtures = new ArrayList<>(generateRoundRobinFixtures(participants));

    final var winsByPlayer = new LinkedHashMap<PlayerId, Integer>();

    for (final var participant : participants) {
      winsByPlayer.put(participant, 0);
    }

    final var tournament = new Tournament(TournamentId.generate(), participants, fixtures,
        winsByPlayer, TournamentStatus.IN_PROGRESS);
    LOGGER.info("Tournament created: tournamentId={}, participants={}, fixtures={}",
        tournament.getId(), participants.size(), fixtures.size());
    return tournament;
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

  public TournamentStatus getStatus() {

    return this.status;
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

  private static final class Fixture {

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

    private static Fixture pending(final FixtureId id, final int matchdayNumber,
        final PlayerId playerOne, final PlayerId playerTwo) {

      return new Fixture(id, matchdayNumber, playerOne, playerTwo, FixtureStatus.PENDING);
    }

    private static Fixture free(final FixtureId id, final int matchdayNumber,
        final PlayerId freePlayer) {

      return new Fixture(id, matchdayNumber, freePlayer, null, FixtureStatus.LIBRE);
    }

    private FixtureId id() {

      return this.id;
    }

    private MatchId matchId() {

      return this.matchId;
    }

    private FixtureStatus status() {

      return this.status;
    }

    private int matchdayNumber() {

      return this.matchdayNumber;
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

    private FixtureView toView() {

      return new FixtureView(this.id, this.matchdayNumber, this.playerOne, this.playerTwo,
          this.matchId, this.winner, this.status);
    }

  }

}
