package com.villo.truco.domain.model.league;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.league.valueobjects.LeagueStatus;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("League.cancel()")
class LeagueCancelTest {

  private League waitingForPlayersLeague() {

    return League.create(PlayerId.generate(), 3, GamesToPlay.of(3));
  }

  private League waitingForStartLeague() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var league = League.create(p1, 3, GamesToPlay.of(3));
    league.join(p2, league.getInviteCode());
    league.join(p3, league.getInviteCode());
    return league;
  }

  private League inProgressLeague() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var league = League.create(p1, 3, GamesToPlay.of(3));
    league.join(p2, league.getInviteCode());
    league.join(p3, league.getInviteCode());
    league.start(p1);
    return league;
  }

  @Test
  @DisplayName("WAITING_FOR_PLAYERS → cancel → CANCELLED")
  void waitingForPlayersIsCancelled() {

    final var league = waitingForPlayersLeague();

    league.cancel();

    assertThat(league.getStatus()).isEqualTo(LeagueStatus.CANCELLED);
  }

  @Test
  @DisplayName("WAITING_FOR_START → cancel → CANCELLED")
  void waitingForStartIsCancelled() {

    final var league = waitingForStartLeague();

    league.cancel();

    assertThat(league.getStatus()).isEqualTo(LeagueStatus.CANCELLED);
  }

  @Test
  @DisplayName("IN_PROGRESS → cancel → no-op")
  void inProgressIsIgnored() {

    final var league = inProgressLeague();

    league.cancel();

    assertThat(league.getStatus()).isEqualTo(LeagueStatus.IN_PROGRESS);
  }

  @Test
  @DisplayName("CANCELLED → cancel → no-op")
  void alreadyCancelledIsIgnored() {

    final var league = waitingForPlayersLeague();
    league.cancel();

    league.cancel();

    assertThat(league.getStatus()).isEqualTo(LeagueStatus.CANCELLED);
  }

}
