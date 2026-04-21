package com.villo.truco.domain.model.league;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("League.hasPlayerPendingFixtures")
class LeaguePlayerAvailabilityTest {

  private static League createStartedLeague(final PlayerId... players) {

    final var league = League.create(players[0], players.length, GamesToPlay.of(3),
        Visibility.PRIVATE);
    for (int i = 1; i < players.length; i++) {
      league.join(players[i]);
    }
    league.start(players[0]);
    return league;
  }

  @Test
  @DisplayName("jugador con fixtures PENDING → true")
  void returnsTrueWhenPlayerHasPendingFixtures() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var league = createStartedLeague(p1, p2, p3);

    assertThat(league.hasPlayerPendingFixtures(p1)).isTrue();
    assertThat(league.hasPlayerPendingFixtures(p2)).isTrue();
  }

  @Test
  @DisplayName("jugador que forfeitó no tiene fixtures PENDING → false")
  void returnsFalseAfterForfeit() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var league = createStartedLeague(p1, p2, p3);

    league.forfeitPlayer(p1);

    assertThat(league.hasPlayerPendingFixtures(p1)).isFalse();
  }

  @Test
  @DisplayName("jugador que no está en la liga → false")
  void returnsFalseForPlayerNotInLeague() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var outsider = PlayerId.generate();
    final var league = createStartedLeague(p1, p2, p3);

    assertThat(league.hasPlayerPendingFixtures(outsider)).isFalse();
  }

  @Test
  @DisplayName("todos los fixtures resueltos → false")
  void returnsFalseWhenAllFixturesFinished() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var league = createStartedLeague(p1, p2, p3);

    league.forfeitPlayer(p1);
    league.forfeitPlayer(p2);

    assertThat(league.hasPlayerPendingFixtures(p3)).isFalse();
  }

}
