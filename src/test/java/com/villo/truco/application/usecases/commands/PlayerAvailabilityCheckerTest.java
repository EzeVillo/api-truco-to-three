package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.exceptions.PlayerBusyInCupException;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.exceptions.PlayerBusyInLeagueException;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.exceptions.PlayerAlreadyInActiveMatchException;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PlayerAvailabilityChecker")
class PlayerAvailabilityCheckerTest {

  private static PlayerAvailabilityChecker checker(final boolean hasUnfinishedMatch,
      final Optional<League> league, final Optional<Cup> cup) {

    final MatchQueryRepository matchRepo = new StubMatchQueryRepository(hasUnfinishedMatch);
    final LeagueQueryRepository leagueRepo = new LeagueQueryRepository() {

      @Override
      public Optional<League> findById(final LeagueId leagueId) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findByInviteCode(final InviteCode inviteCode) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findByMatchId(final MatchId matchId) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findInProgressByPlayer(final PlayerId playerId) {

        return league;
      }
    };
    final CupQueryRepository cupRepo = new CupQueryRepository() {

      @Override
      public Optional<Cup> findById(final CupId cupId) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findByInviteCode(final InviteCode inviteCode) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findByMatchId(final MatchId matchId) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findInProgressByPlayer(final PlayerId playerId) {

        return cup;
      }
    };
    return new PlayerAvailabilityChecker(matchRepo, leagueRepo, cupRepo);
  }

  @Test
  @DisplayName("jugador libre pasa sin excepción")
  void freePlayerPassesWithNoException() {

    final var checker = checker(false, Optional.empty(), Optional.empty());

    assertThatCode(() -> checker.ensureAvailable(PlayerId.generate())).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("jugador con match sin finalizar → PlayerAlreadyInActiveMatchException")
  void throwsWhenPlayerHasUnfinishedMatch() {

    final var checker = checker(true, Optional.empty(), Optional.empty());

    assertThatThrownBy(() -> checker.ensureAvailable(PlayerId.generate())).isInstanceOf(
        PlayerAlreadyInActiveMatchException.class);
  }

  @Test
  @DisplayName("jugador con fixtures pendientes en liga → PlayerBusyInLeagueException")
  void throwsWhenPlayerBusyInLeague() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var league = League.create(p1, 3, GamesToPlay.of(3));
    league.join(p2, league.getInviteCode());
    league.join(p3, league.getInviteCode());
    league.start(p1);

    final var checker = checker(false, Optional.of(league), Optional.empty());

    assertThatThrownBy(() -> checker.ensureAvailable(p1)).isInstanceOf(
        PlayerBusyInLeagueException.class);
  }

  @Test
  @DisplayName("jugador que forfeitó en liga no está bloqueado")
  void forfeitedLeaguePlayerIsNotBlocked() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var league = League.create(p1, 3, GamesToPlay.of(3));
    league.join(p2, league.getInviteCode());
    league.join(p3, league.getInviteCode());
    league.start(p1);
    league.forfeitPlayer(p1);

    final var checker = checker(false, Optional.of(league), Optional.empty());

    assertThatCode(() -> checker.ensureAvailable(p1)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("jugador activo en copa → PlayerBusyInCupException")
  void throwsWhenPlayerStillCompetingInCup() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var p4 = PlayerId.generate();
    final var cup = Cup.create(p1, 4, GamesToPlay.of(3));
    cup.join(p2, cup.getInviteCode());
    cup.join(p3, cup.getInviteCode());
    cup.join(p4, cup.getInviteCode());
    cup.start(p1);

    final var checker = checker(false, Optional.empty(), Optional.of(cup));

    assertThatThrownBy(() -> checker.ensureAvailable(p1)).isInstanceOf(
        PlayerBusyInCupException.class);
  }

  @Test
  @DisplayName("jugador forfeitado en copa no está bloqueado")
  void forfeitedCupPlayerIsNotBlocked() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var p4 = PlayerId.generate();
    final var cup = Cup.create(p1, 4, GamesToPlay.of(3));
    cup.join(p2, cup.getInviteCode());
    cup.join(p3, cup.getInviteCode());
    cup.join(p4, cup.getInviteCode());
    cup.start(p1);
    cup.forfeitPlayer(p1);

    final var checker = checker(false, Optional.empty(), Optional.of(cup));

    assertThatCode(() -> checker.ensureAvailable(p1)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("short-circuit: match bloqueante no evalúa liga ni copa")
  void shortCircuitsOnMatchCheck() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var league = League.create(p1, 3, GamesToPlay.of(3));
    league.join(p2, league.getInviteCode());
    league.join(p3, league.getInviteCode());
    league.start(p1);

    final var checker = checker(true, Optional.of(league), Optional.empty());

    assertThatThrownBy(() -> checker.ensureAvailable(p1)).isInstanceOf(
        PlayerAlreadyInActiveMatchException.class);
  }

  private record StubMatchQueryRepository(boolean unfinished) implements MatchQueryRepository {

    @Override
      public Optional<Match> findById(final MatchId matchId) {

        return Optional.empty();
      }

      @Override
      public Optional<Match> findByInviteCode(final InviteCode inviteCode) {

        return Optional.empty();
      }

      @Override
      public boolean hasActiveMatch(final PlayerId playerId) {

        return false;
      }

      @Override
      public boolean hasUnfinishedMatch(final PlayerId playerId) {

        return this.unfinished;
      }

      @Override
      public List<MatchId> findIdleMatchIds(final Instant idleSince) {

        return List.of();
      }

    }

}
