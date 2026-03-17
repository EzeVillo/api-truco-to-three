package com.villo.truco.domain.model.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.tournament.Tournament.FixtureView;
import com.villo.truco.domain.model.tournament.valueobjects.FixtureStatus;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentStatus;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TournamentTest {

  private static FixtureView findFixture(final List<FixtureView> fixtures, final PlayerId left,
      final PlayerId right) {

    return fixtures.stream().filter(
            fixture -> (fixture.playerOne().equals(left) && fixture.playerTwo().equals(right)) || (
                fixture.playerOne().equals(right) && fixture.playerTwo().equals(left))).findFirst()
        .orElseThrow();
  }

  private static Tournament createStartedTournament(final PlayerId... players) {

    final var tournament = Tournament.create(players[0], players.length, GamesToPlay.of(3));
    for (int i = 1; i < players.length; i++) {
      tournament.join(players[i], tournament.getInviteCode());
    }
    tournament.start(players[0]);
    return tournament;
  }

  @Test
  @DisplayName("genera calendario por fechas con 1 partido por jugador")
  void generatesRoundRobinMatchdaysForEvenPlayers() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var p4 = PlayerId.generate();

    final var tournament = createStartedTournament(p1, p2, p3, p4);

    assertThat(tournament.getMatchdays()).hasSize(3);
    assertThat(tournament.getFixtures()).hasSize(6);

    for (final var matchday : tournament.getMatchdays()) {
      assertThat(matchday.fixtures()).hasSize(2);
      assertThat(matchday.fixtures()).allMatch(fixture -> fixture.status() != FixtureStatus.LIBRE);

      final var seenPlayers = new HashSet<PlayerId>();

      for (final var fixture : matchday.fixtures()) {
        assertThat(seenPlayers.add(fixture.playerOne())).isTrue();
        assertThat(seenPlayers.add(fixture.playerTwo())).isTrue();
      }

      assertThat(seenPlayers).hasSize(4);
    }

    assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.IN_PROGRESS);
  }

  @Test
  @DisplayName("si hay impares, hay un jugador libre por fecha")
  void createsOneFreePlayerPerMatchdayWhenOddPlayers() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();

    final var tournament = createStartedTournament(p1, p2, p3);

    assertThat(tournament.getMatchdays()).hasSize(3);

    for (final var matchday : tournament.getMatchdays()) {
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

    final var tournament = createStartedTournament(p1, p2, p3);

    final var playableFixtures = tournament.getFixtures().stream()
        .filter(fixture -> fixture.status() == FixtureStatus.PENDING).toList();

    final var fixture12 = findFixture(playableFixtures, p1, p2);
    final var fixture23 = findFixture(playableFixtures, p2, p3);
    final var fixture13 = findFixture(playableFixtures, p1, p3);

    final var m12 = MatchId.generate();
    final var m23 = MatchId.generate();
    final var m13 = MatchId.generate();

    tournament.linkFixtureMatch(fixture12.fixtureId(), m12);
    tournament.linkFixtureMatch(fixture23.fixtureId(), m23);
    tournament.linkFixtureMatch(fixture13.fixtureId(), m13);

    tournament.recordMatchWinner(m12, p1);
    tournament.recordMatchWinner(m23, p2);
    tournament.recordMatchWinner(m13, p3);

    assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.FINISHED);
    assertThat(tournament.getLeaders()).hasSize(3);
    assertThat(tournament.getWinsByPlayer().values()).containsOnly(1);
  }

  @Test
  @DisplayName("suma victoria al ganador del fixture")
  void recordsWinnerWin() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();

    final var tournament = createStartedTournament(p1, p2, p3);

    final var pendingFixtures = tournament.getFixtures().stream()
        .filter(it -> it.status() == FixtureStatus.PENDING).toList();

    final var fixture12 = findFixture(pendingFixtures, p1, p2);
    final var fixture23 = findFixture(pendingFixtures, p2, p3);
    final var fixture13 = findFixture(pendingFixtures, p1, p3);

    final var m12 = MatchId.generate();
    final var m23 = MatchId.generate();
    final var m13 = MatchId.generate();

    tournament.linkFixtureMatch(fixture12.fixtureId(), m12);
    tournament.linkFixtureMatch(fixture23.fixtureId(), m23);
    tournament.linkFixtureMatch(fixture13.fixtureId(), m13);

    tournament.recordMatchWinner(m12, p1);

    assertThat(tournament.getWinsByPlayer().get(p1)).isEqualTo(1);
    assertThat(tournament.getWinsByPlayer().get(p2)).isZero();
    assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.IN_PROGRESS);

    tournament.recordMatchWinner(m23, p2);
    tournament.recordMatchWinner(m13, p1);

    assertThat(tournament.getWinsByPlayer().get(p1)).isEqualTo(2);
    assertThat(tournament.getWinsByPlayer().get(p2)).isEqualTo(1);
    assertThat(tournament.getWinsByPlayer().get(p3)).isZero();
    assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.FINISHED);
    assertThat(tournament.getLeaders()).containsExactly(p1);
  }

}
