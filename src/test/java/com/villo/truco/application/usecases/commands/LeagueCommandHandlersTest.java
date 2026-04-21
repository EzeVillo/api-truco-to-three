package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.commands.CreateLeagueCommand;
import com.villo.truco.application.commands.LeaveLeagueCommand;
import com.villo.truco.application.commands.StartLeagueCommand;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.events.LeagueDomainEvent;
import com.villo.truco.domain.model.league.events.PublicLeagueLobbyOpenedEvent;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("League command handlers")
class LeagueCommandHandlersTest {

  private PlayerAvailabilityChecker availableChecker() {

    final MatchQueryRepository matchQueryRepository = new MatchQueryRepository() {
      @Override
      public Optional<Match> findById(final MatchId matchId) {

        return Optional.empty();
      }

      @Override
      public boolean hasActiveMatch(final PlayerId playerId) {

        return false;
      }

      @Override
      public boolean hasUnfinishedMatch(final PlayerId playerId) {

        return false;
      }

      @Override
      public List<MatchId> findIdleMatchIds(final Instant idleSince) {

        return List.of();
      }

      @Override
      public List<Match> findPublicWaiting() {

        return List.of();
      }

      @Override
      public CursorPageResult<Match> findPublicWaiting(final CursorPageQuery pageQuery) {

        return new CursorPageResult<>(findPublicWaiting(), null);
      }
    };
    final LeagueQueryRepository leagueQueryRepository = new LeagueQueryRepository() {
      @Override
      public Optional<League> findById(final LeagueId leagueId) {

        return Optional.empty();
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

        return new CursorPageResult<>(findPublicWaiting(), null);
      }
    };
    final CupQueryRepository cupQueryRepository = new CupQueryRepository() {
      @Override
      public Optional<Cup> findById(final CupId cupId) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findByMatchId(final MatchId matchId) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findInProgressByPlayer(final PlayerId playerId) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findWaitingByPlayer(final PlayerId playerId) {

        return Optional.empty();
      }

      @Override
      public List<CupId> findIdleCupIds(final Instant idleSince) {

        return List.of();
      }

      private List<Cup> findPublicWaiting() {

        return List.of();
      }

      @Override
      public CursorPageResult<Cup> findPublicWaiting(final CursorPageQuery pageQuery) {

        return new CursorPageResult<>(findPublicWaiting(), null);
      }
    };
    final BotRegistry noBotRegistry = new BotRegistry() {
      @Override
      public boolean isBot(final PlayerId p) {

        return false;
      }

      @Override
      public Optional<BotProfile> getProfile(final PlayerId p) {

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
    return new PlayerAvailabilityChecker(matchQueryRepository, leagueQueryRepository,
        cupQueryRepository, noBotRegistry);
  }

  @Test
  @DisplayName("CreateLeagueCommandHandler crea y persiste league")
  void createLeagueHandlerCreatesAndSaves() {

    final var saved = new AtomicReference<League>();
    final var publishedEvents = new ArrayList<LeagueDomainEvent>();
    final var handler = new CreateLeagueCommandHandler(saved::set, publishedEvents::addAll,
        availableChecker());
    final var creator = PlayerId.generate();

    final var result = handler.handle(
        new CreateLeagueCommand(creator, 3, GamesToPlay.of(3), Visibility.PUBLIC));

    assertThat(saved.get()).isNotNull();
    assertThat(result.leagueId()).isEqualTo(saved.get().getId().value().toString());
    assertThat(result.joinCode()).isEqualTo(saved.get().getJoinCode().value());
    assertThat(result.joinCode()).isNotBlank();
    assertThat(publishedEvents).hasSize(1);
    assertThat(publishedEvents.getFirst()).isInstanceOf(PublicLeagueLobbyOpenedEvent.class);
    assertThat(saved.get().getLeagueDomainEvents()).isEmpty();
  }

  @Test
  @DisplayName("LeaveLeagueCommandHandler saca jugador y persiste")
  void leaveLeagueHandlerLeavesAndSaves() {

    final var creator = PlayerId.generate();
    final var leaver = PlayerId.generate();
    final var league = League.create(creator, 3, GamesToPlay.of(3), Visibility.PRIVATE);
    league.join(leaver);

    final LeagueQueryRepository queryRepository = new LeagueQueryRepository() {
      @Override
      public Optional<League> findById(final LeagueId leagueId) {

        return Optional.of(league);
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

        return new CursorPageResult<>(findPublicWaiting(), null);
      }
    };

    final var saved = new AtomicReference<League>();
    final var handler = new LeaveLeagueCommandHandler(new LeagueResolver(queryRepository),
        saved::set, events -> {
    });

    handler.handle(new LeaveLeagueCommand(league.getId(), leaver));

    assertThat(saved.get()).isSameAs(league);
  }

  @Test
  @DisplayName("StartLeagueCommandHandler crea matches para fixtures pendientes y persiste")
  void startLeagueHandlerCreatesMatchesAndSaves() {

    final var p1 = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var league = League.create(p1, 3, GamesToPlay.of(3), Visibility.PRIVATE);
    league.join(p2);
    league.join(p3);

    final LeagueQueryRepository queryRepository = new LeagueQueryRepository() {
      @Override
      public Optional<League> findById(final LeagueId leagueId) {

        return Optional.of(league);
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

        return new CursorPageResult<>(findPublicWaiting(), null);
      }
    };

    final var leagueSaved = new AtomicReference<League>();
    final var matchSaves = new AtomicInteger();
    final MatchRepository matchRepository = match -> matchSaves.incrementAndGet();

    final var handler = new StartLeagueCommandHandler(new LeagueResolver(queryRepository),
        leagueSaved::set, matchRepository, events -> {
    });

    handler.handle(new StartLeagueCommand(league.getId(), p1));

    final var linkedCount = league.getFixtures().stream().filter(f -> f.matchId() != null).count();

    assertThat(leagueSaved.get()).isSameAs(league);
    assertThat(matchSaves.get()).isEqualTo((int) linkedCount);
    assertThat(matchSaves.get()).isGreaterThan(0);
  }

}
