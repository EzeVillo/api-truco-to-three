package com.villo.truco.domain.model.league;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.league.valueobjects.FixtureStatus;
import com.villo.truco.domain.model.league.valueobjects.LeagueStatus;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LeagueTest {

  private static FixtureView findFixture(final List<FixtureView> fixtures, final PlayerId left,
      final PlayerId right) {

    return fixtures.stream().filter(
            fixture -> (fixture.playerOne().equals(left) && fixture.playerTwo().equals(right)) || (
                fixture.playerOne().equals(right) && fixture.playerTwo().equals(left))).findFirst()
        .orElseThrow();
  }

  private static League createStartedLeague(final PlayerId... players) {

    final var league = League.create(players[0], players.length, GamesToPlay.of(3));
    for (int i = 1; i < players.length; i++) {
      league.join(players[i], league.getInviteCode());
    }
    league.start(players[0]);
    return league;
  }

  @Test
  @DisplayName("genera calendario por fechas con 1 partido por jugador")
  void generatesRoundRobinMatchdaysForEvenPlayers() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var p4 = PlayerId.generate();

    final var league = createStartedLeague(p1, p2, p3, p4);

    assertThat(league.getMatchdays()).hasSize(3);
    assertThat(league.getFixtures()).hasSize(6);

    for (final var matchday : league.getMatchdays()) {
      assertThat(matchday.fixtures()).hasSize(2);
      assertThat(matchday.fixtures()).allMatch(fixture -> fixture.status() != FixtureStatus.LIBRE);

      final var seenPlayers = new HashSet<PlayerId>();

      for (final var fixture : matchday.fixtures()) {
        assertThat(seenPlayers.add(fixture.playerOne())).isTrue();
        assertThat(seenPlayers.add(fixture.playerTwo())).isTrue();
      }

      assertThat(seenPlayers).hasSize(4);
    }

    assertThat(league.getStatus()).isEqualTo(LeagueStatus.IN_PROGRESS);
  }

  @Test
  @DisplayName("si hay impares, hay un jugador libre por fecha")
  void createsOneFreePlayerPerMatchdayWhenOddPlayers() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();

    final var league = createStartedLeague(p1, p2, p3);

    assertThat(league.getMatchdays()).hasSize(3);

    for (final var matchday : league.getMatchdays()) {
      assertThat(matchday.fixtures()).hasSize(2);
      assertThat(matchday.fixtures().stream()
          .filter(fixture -> fixture.status() == FixtureStatus.LIBRE)).hasSize(1);
      assertThat(matchday.fixtures().stream()
          .filter(fixture -> fixture.status() == FixtureStatus.PENDING)).hasSize(1);
    }
  }

  @Test
  @DisplayName("termina empatado cuando todos ganan un partido")
  void finishesWithTieWhenLeadersHaveSameWins() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();

    final var league = createStartedLeague(p1, p2, p3);

    final var playableFixtures = league.getFixtures().stream()
        .filter(fixture -> fixture.status() == FixtureStatus.PENDING).toList();

    final var fixture12 = findFixture(playableFixtures, p1, p2);
    final var fixture23 = findFixture(playableFixtures, p2, p3);
    final var fixture13 = findFixture(playableFixtures, p1, p3);

    final var m12 = MatchId.generate();
    final var m23 = MatchId.generate();
    final var m13 = MatchId.generate();

    league.linkFixtureMatch(fixture12.fixtureId(), m12);
    league.linkFixtureMatch(fixture23.fixtureId(), m23);
    league.linkFixtureMatch(fixture13.fixtureId(), m13);

    league.recordMatchWinner(m12, p1);
    league.recordMatchWinner(m23, p2);
    league.recordMatchWinner(m13, p3);

    assertThat(league.getStatus()).isEqualTo(LeagueStatus.FINISHED);
    assertThat(league.getLeaders()).hasSize(3);
    assertThat(league.getWinsByPlayer().values()).containsOnly(1);
  }

  @Test
  @DisplayName("suma victoria al ganador del fixture")
  void recordsWinnerWin() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();

    final var league = createStartedLeague(p1, p2, p3);

    final var pendingFixtures = league.getFixtures().stream()
        .filter(it -> it.status() == FixtureStatus.PENDING).toList();

    final var fixture12 = findFixture(pendingFixtures, p1, p2);
    final var fixture23 = findFixture(pendingFixtures, p2, p3);
    final var fixture13 = findFixture(pendingFixtures, p1, p3);

    final var m12 = MatchId.generate();
    final var m23 = MatchId.generate();
    final var m13 = MatchId.generate();

    league.linkFixtureMatch(fixture12.fixtureId(), m12);
    league.linkFixtureMatch(fixture23.fixtureId(), m23);
    league.linkFixtureMatch(fixture13.fixtureId(), m13);

    league.recordMatchWinner(m12, p1);

    assertThat(league.getWinsByPlayer().get(p1)).isEqualTo(1);
    assertThat(league.getWinsByPlayer().get(p2)).isZero();
    assertThat(league.getStatus()).isEqualTo(LeagueStatus.IN_PROGRESS);

    league.recordMatchWinner(m23, p2);
    league.recordMatchWinner(m13, p1);

    assertThat(league.getWinsByPlayer().get(p1)).isEqualTo(2);
    assertThat(league.getWinsByPlayer().get(p2)).isEqualTo(1);
    assertThat(league.getWinsByPlayer().get(p3)).isZero();
    assertThat(league.getStatus()).isEqualTo(LeagueStatus.FINISHED);
    assertThat(league.getLeaders()).containsExactly(p1);
  }

  @Test
  @DisplayName("ningún jugador es siempre playerOne en todos sus fixtures")
  void noPlayerIsAlwaysPlayerOneInAllFixtures() {

    final var players = new PlayerId[]{PlayerId.generate(), PlayerId.generate(),
        PlayerId.generate(), PlayerId.generate()};

    final var league = createStartedLeague(players);

    final var pendingFixtures = league.getFixtures().stream()
        .filter(f -> f.status() == FixtureStatus.PENDING).toList();

    for (final var player : players) {
      final var fixturesAsPlayerOne = pendingFixtures.stream()
          .filter(f -> f.playerOne().equals(player)).count();
      final var fixturesAsPlayerTwo = pendingFixtures.stream()
          .filter(f -> f.playerTwo().equals(player)).count();

      assertThat(fixturesAsPlayerOne).as("Player %s should not be playerOne in ALL fixtures",
          player).isLessThan(fixturesAsPlayerOne + fixturesAsPlayerTwo);
    }
  }

}
