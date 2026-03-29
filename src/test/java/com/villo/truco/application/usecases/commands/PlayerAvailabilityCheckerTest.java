package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.exceptions.PlayerAlreadyInWaitingCupException;
import com.villo.truco.domain.model.cup.exceptions.PlayerBusyInCupException;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.exceptions.PlayerAlreadyInWaitingLeagueException;
import com.villo.truco.domain.model.league.exceptions.PlayerBusyInLeagueException;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.exceptions.PlayerAlreadyInActiveMatchException;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.MatchId;
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

    return checker(hasUnfinishedMatch, false, league, Optional.empty(), cup, Optional.empty());
  }

  private static PlayerAvailabilityChecker checker(final boolean hasUnfinishedMatch,
      final Optional<League> inProgressLeague, final Optional<League> waitingLeague,
      final Optional<Cup> inProgressCup, final Optional<Cup> waitingCup) {

    return checker(hasUnfinishedMatch, false, inProgressLeague, waitingLeague, inProgressCup,
        waitingCup);
  }

  private static PlayerAvailabilityChecker checker(final boolean hasUnfinishedMatch,
      final boolean hasActiveMatch, final Optional<League> inProgressLeague,
      final Optional<League> waitingLeague, final Optional<Cup> inProgressCup,
      final Optional<Cup> waitingCup) {

    final MatchQueryRepository matchRepo = new StubMatchQueryRepository(hasUnfinishedMatch,
        hasActiveMatch);
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

        return inProgressLeague;
      }

      @Override
      public Optional<League> findWaitingByPlayer(final PlayerId playerId) {

        return waitingLeague;
      }

      @Override
      public List<LeagueId> findIdleLeagueIds(final Instant idleSince) {

        return List.of();
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

        return inProgressCup;
      }

      @Override
      public Optional<Cup> findWaitingByPlayer(final PlayerId playerId) {

        return waitingCup;
      }

      @Override
      public List<CupId> findIdleCupIds(final Instant idleSince) {

        return List.of();
      }
    };
    final BotRegistry noBotRegistry = new BotRegistry() {

      @Override
      public boolean isBot(final PlayerId playerId) {

        return false;
      }

      @Override
      public Optional<BotProfile> getProfile(final PlayerId playerId) {

        return Optional.empty();
      }

      @Override
      public List<BotProfile> getAll() {

        return List.of();
      }

      @Override
      public void register(final BotProfile profile) {

      }
    };
    return new PlayerAvailabilityChecker(matchRepo, leagueRepo, cupRepo, noBotRegistry);
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

  @Test
  @DisplayName("jugador en liga WAITING_FOR_PLAYERS → PlayerAlreadyInWaitingLeagueException")
  void throwsWhenPlayerInWaitingLeague() {

    final var creator = PlayerId.generate();
    final var waitingLeague = League.create(creator, 3, GamesToPlay.of(3));
    final var checker = checker(false, Optional.empty(), Optional.of(waitingLeague),
        Optional.empty(), Optional.empty());

    assertThatThrownBy(() -> checker.ensureAvailable(creator)).isInstanceOf(
        PlayerAlreadyInWaitingLeagueException.class);
  }

  @Test
  @DisplayName("jugador en copa WAITING_FOR_PLAYERS → PlayerAlreadyInWaitingCupException")
  void throwsWhenPlayerInWaitingCup() {

    final var creator = PlayerId.generate();
    final var waitingCup = Cup.create(creator, 4, GamesToPlay.of(3));
    final var checker = checker(false, Optional.empty(), Optional.empty(), Optional.empty(),
        Optional.of(waitingCup));

    assertThatThrownBy(() -> checker.ensureAvailable(creator)).isInstanceOf(
        PlayerAlreadyInWaitingCupException.class);
  }

  @Test
  @DisplayName("ensureCanStartMatch: jugador libre pasa sin excepción")
  void canStartMatchWhenFree() {

    final var checker = checker(false, false, Optional.empty(), Optional.empty(), Optional.empty(),
        Optional.empty());

    assertThatCode(
        () -> checker.ensureCanStartMatch(PlayerId.generate())).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("ensureCanStartMatch: jugador con partido activo → PlayerAlreadyInActiveMatchException")
  void cannotStartMatchWhenHasActiveMatch() {

    final var checker = checker(false, true, Optional.empty(), Optional.empty(), Optional.empty(),
        Optional.empty());

    assertThatThrownBy(() -> checker.ensureCanStartMatch(PlayerId.generate())).isInstanceOf(
        PlayerAlreadyInActiveMatchException.class);
  }

  @Test
  @DisplayName("ensureCanStartMatch: jugador en torneo activo (IN_PROGRESS) puede iniciar su partida de torneo")
  void canStartTournamentMatchWhenInActiveTournament() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var league = League.create(p1, 3, GamesToPlay.of(3));
    league.join(p2, league.getInviteCode());
    league.join(p3, league.getInviteCode());
    league.start(p1);

    final var checker = checker(false, false, Optional.of(league), Optional.empty(),
        Optional.empty(), Optional.empty());

    assertThatCode(() -> checker.ensureCanStartMatch(p1)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("ensureCanStartMatch: jugador en torneo en espera no puede iniciar un match")
  void cannotStartMatchWhenInWaitingTournament() {

    final var creator = PlayerId.generate();
    final var waitingLeague = League.create(creator, 3, GamesToPlay.of(3));

    final var checker = checker(false, false, Optional.empty(), Optional.of(waitingLeague),
        Optional.empty(), Optional.empty());

    assertThatThrownBy(() -> checker.ensureCanStartMatch(creator)).isInstanceOf(
        PlayerAlreadyInWaitingLeagueException.class);
  }

  private record StubMatchQueryRepository(boolean unfinished, boolean activeMatch) implements
      MatchQueryRepository {

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

      return this.activeMatch;
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
