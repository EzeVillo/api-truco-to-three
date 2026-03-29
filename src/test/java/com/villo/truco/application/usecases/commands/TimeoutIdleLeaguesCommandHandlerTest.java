package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.ports.TransactionalRunner;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.league.valueobjects.LeagueStatus;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.LeagueRepository;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TimeoutIdleLeaguesCommandHandler")
class TimeoutIdleLeaguesCommandHandlerTest {

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

  private League finishedLeague() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var league = League.create(p1, 3, GamesToPlay.of(3));
    league.join(p2, league.getInviteCode());
    league.join(p3, league.getInviteCode());
    league.start(p1);
    league.forfeitPlayer(p2);
    league.forfeitPlayer(p3);
    return league;
  }

  private TimeoutIdleLeaguesCommandHandler handlerFor(final Map<LeagueId, League> leagues,
      final AtomicReference<League> savedLeague) {

    final LeagueQueryRepository queryRepo = new LeagueQueryRepository() {

      @Override
      public Optional<League> findById(final LeagueId leagueId) {

        return Optional.ofNullable(leagues.get(leagueId));
      }

      @Override
      public Optional<League> findByInviteCode(final InviteCode inviteCode) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findByMatchId(
          final com.villo.truco.domain.shared.valueobjects.MatchId matchId) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findInProgressByPlayer(final PlayerId playerId) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findWaitingByPlayer(final PlayerId playerId) {

        return Optional.empty();
      }

      @Override
      public List<LeagueId> findIdleLeagueIds(final Instant idleSince) {

        return List.copyOf(leagues.keySet());
      }
    };

    final LeagueRepository leagueRepository = savedLeague::set;
    final TransactionalRunner transactionalRunner = Runnable::run;

    return new TimeoutIdleLeaguesCommandHandler(queryRepo, leagueRepository, transactionalRunner,
        Duration.ofMinutes(10), events -> {
    });
  }

  @Test
  @DisplayName("liga en WAITING_FOR_PLAYERS se cancela")
  void waitingForPlayersLeagueIsCancelled() {

    final var league = waitingForPlayersLeague();
    final var savedLeague = new AtomicReference<League>();
    final var handler = handlerFor(Map.of(league.getId(), league), savedLeague);

    handler.handle();

    assertThat(savedLeague.get()).isNotNull();
    assertThat(savedLeague.get().getStatus()).isEqualTo(LeagueStatus.CANCELLED);
  }

  @Test
  @DisplayName("liga en WAITING_FOR_START se cancela")
  void waitingForStartLeagueIsCancelled() {

    final var league = waitingForStartLeague();
    final var savedLeague = new AtomicReference<League>();
    final var handler = handlerFor(Map.of(league.getId(), league), savedLeague);

    handler.handle();

    assertThat(savedLeague.get()).isNotNull();
    assertThat(savedLeague.get().getStatus()).isEqualTo(LeagueStatus.CANCELLED);
  }

  @Test
  @DisplayName("liga ya FINISHED es ignorada")
  void finishedLeagueIsSkipped() {

    final var league = finishedLeague();
    final var savedLeague = new AtomicReference<League>();
    final var handler = handlerFor(Map.of(league.getId(), league), savedLeague);

    handler.handle();

    assertThat(savedLeague.get()).isNull();
  }

  @Test
  @DisplayName("lista vacía de idle leagues no hace nada")
  void emptyIdleListDoesNothing() {

    final var savedLeague = new AtomicReference<League>();
    final var handler = handlerFor(Map.of(), savedLeague);

    handler.handle();

    assertThat(savedLeague.get()).isNull();
  }

  @Test
  @DisplayName("excepción en una liga no afecta las demás")
  void exceptionInOneLeagueDoesNotAffectOthers() {

    final var goodLeague = waitingForPlayersLeague();
    final var savedLeague = new AtomicReference<League>();

    final LeagueQueryRepository queryRepo = new LeagueQueryRepository() {

      @Override
      public Optional<League> findById(final LeagueId leagueId) {

        if (leagueId.equals(goodLeague.getId())) {
          return Optional.of(goodLeague);
        }
        throw new RuntimeException("simulated failure");
      }

      @Override
      public Optional<League> findByInviteCode(final InviteCode inviteCode) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findByMatchId(
          final com.villo.truco.domain.shared.valueobjects.MatchId matchId) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findInProgressByPlayer(final PlayerId playerId) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findWaitingByPlayer(final PlayerId playerId) {

        return Optional.empty();
      }

      @Override
      public List<LeagueId> findIdleLeagueIds(final Instant idleSince) {

        return List.of(LeagueId.generate(), goodLeague.getId());
      }
    };

    final var handler = new TimeoutIdleLeaguesCommandHandler(queryRepo, savedLeague::set,
        Runnable::run, Duration.ofMinutes(10), events -> {
    });

    handler.handle();

    assertThat(savedLeague.get()).isNotNull();
    assertThat(savedLeague.get().getStatus()).isEqualTo(LeagueStatus.CANCELLED);
  }

}
