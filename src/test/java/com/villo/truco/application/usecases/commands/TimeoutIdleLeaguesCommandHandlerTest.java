package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.ports.RetryableTransactionalRunner;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.league.valueobjects.LeagueStatus;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.LeagueRepository;
import com.villo.truco.domain.ports.LeagueTimeoutEntry;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TimeoutIdleLeaguesCommandHandler")
class TimeoutIdleLeaguesCommandHandlerTest {

  private League waitingForPlayersLeague() {

    return League.create(PlayerId.generate(), 3, GamesToPlay.of(3), Visibility.PRIVATE);
  }

  private League waitingForStartLeague() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var league = League.create(p1, 3, GamesToPlay.of(3), Visibility.PRIVATE);
    league.join(p2);
    league.join(p3);
    return league;
  }

  private League finishedLeague() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var league = League.create(p1, 3, GamesToPlay.of(3), Visibility.PRIVATE);
    league.join(p2);
    league.join(p3);
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
      public Optional<League> findByMatchId(final MatchId matchId) {

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

        return List.of();
      }

      @Override
      public List<League> findPublicWaiting() {

        return List.of();
      }

      @Override
      public CursorPageResult<League> findPublicWaiting(final CursorPageQuery pageQuery) {

        return new CursorPageResult<>(List.of(), null);
      }
    };

    final LeagueRepository leagueRepository = new LeagueRepository() {
      @Override
      public void save(final League league) {

        savedLeague.set(league);
      }

      @Override
      public Stream<LeagueTimeoutEntry> findActiveWithTimeoutDeadline() {

        return Stream.empty();
      }
    };
    final RetryableTransactionalRunner transactionalRunner = Runnable::run;

    return new TimeoutIdleLeaguesCommandHandler(queryRepo, leagueRepository, transactionalRunner,
        events -> {
        });
  }

  @Test
  @DisplayName("liga en WAITING_FOR_PLAYERS se cancela")
  void waitingForPlayersLeagueIsCancelled() {

    final var league = waitingForPlayersLeague();
    final var savedLeague = new AtomicReference<League>();
    final var handler = handlerFor(Map.of(league.getId(), league), savedLeague);

    handler.handle(league.getId());

    assertThat(savedLeague.get()).isNotNull();
    assertThat(savedLeague.get().getStatus()).isEqualTo(LeagueStatus.CANCELLED);
  }

  @Test
  @DisplayName("liga en WAITING_FOR_START se cancela")
  void waitingForStartLeagueIsCancelled() {

    final var league = waitingForStartLeague();
    final var savedLeague = new AtomicReference<League>();
    final var handler = handlerFor(Map.of(league.getId(), league), savedLeague);

    handler.handle(league.getId());

    assertThat(savedLeague.get()).isNotNull();
    assertThat(savedLeague.get().getStatus()).isEqualTo(LeagueStatus.CANCELLED);
  }

  @Test
  @DisplayName("liga ya FINISHED es ignorada")
  void finishedLeagueIsSkipped() {

    final var league = finishedLeague();
    final var savedLeague = new AtomicReference<League>();
    final var handler = handlerFor(Map.of(league.getId(), league), savedLeague);

    handler.handle(league.getId());

    assertThat(savedLeague.get()).isNull();
  }

  @Test
  @DisplayName("league inexistente es ignorada")
  void unknownLeagueIdIsIgnored() {

    final var savedLeague = new AtomicReference<League>();
    final var handler = handlerFor(Map.of(), savedLeague);

    handler.handle(LeagueId.generate());

    assertThat(savedLeague.get()).isNull();
  }

}
