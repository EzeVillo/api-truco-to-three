package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.commands.StartMatchCommand;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.in.StartMatchUseCase;
import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.MatchRehydrator;
import com.villo.truco.domain.model.match.MatchSnapshotExtractor;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.exceptions.StaleAggregateException;
import com.villo.truco.infrastructure.pipeline.OptimisticLockRetryBehavior;
import com.villo.truco.infrastructure.pipeline.UseCasePipeline;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

@DisplayName("StartMatchCommandHandler — concurrency")
class StartMatchCommandHandlerConcurrencyTest {

  private final Map<MatchId, Match> store = new ConcurrentHashMap<>();
  private final Map<MatchId, AtomicLong> versions = new ConcurrentHashMap<>();
  private final List<MatchDomainEvent> publishedEvents = Collections.synchronizedList(
      new ArrayList<>());
  private final MatchEventNotifier matchEventNotifier = publishedEvents::addAll;
  private final UseCasePipeline pipeline = new UseCasePipeline(
      List.of(new OptimisticLockRetryBehavior(3, Duration.ZERO)));
  private final MatchRepository matchRepository = match -> {
    synchronized (store) {
      final var currentVersion = versions.computeIfAbsent(match.getId(), k -> new AtomicLong(0));
      if (match.getVersion() != currentVersion.get()) {
        throw new StaleAggregateException(
            "Version mismatch: expected " + currentVersion.get() + " but was " + match.getVersion(),
            null);
      }
      currentVersion.incrementAndGet();
      match.setVersion(currentVersion.get());
      store.put(match.getId(), match);
    }
  };
  private final MatchQueryRepository matchQueryRepository = new MatchQueryRepository() {
    @Override
    public Optional<Match> findById(final MatchId id) {

      return Optional.ofNullable(store.get(id)).map(m -> {
        synchronized (store) {
          final var snapshot = MatchSnapshotExtractor.extract(m);
          final var copy = MatchRehydrator.rehydrate(snapshot);
          copy.setVersion(m.getVersion());
          return copy;
        }
      });
    }

    @Override
    public boolean hasActiveMatch(final PlayerId playerId) {

      return store.values().stream().anyMatch(
          m -> m.getStatus() == MatchStatus.IN_PROGRESS && (playerId.equals(m.getPlayerOne())
              || playerId.equals(m.getPlayerTwo())));
    }

    @Override
    public boolean hasUnfinishedMatch(final PlayerId playerId) {

      return store.values().stream().anyMatch(
          m -> m.getStatus() != MatchStatus.FINISHED && (playerId.equals(m.getPlayerOne())
              || playerId.equals(m.getPlayerTwo())));
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
  private StartMatchUseCase handler;
  private PlayerId playerOne;
  private PlayerId playerTwo;
  private Match match;

  private static void awaitLatch(final CountDownLatch latch) {

    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @BeforeEach
  void setUp() {

    store.clear();
    versions.clear();
    publishedEvents.clear();

    final MatchResolver matchResolver = new MatchResolver(matchQueryRepository);
    final LeagueQueryRepository leagueQueryRepository = new LeagueQueryRepository() {
      @Override
      public Optional<League> findById(LeagueId id) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findByMatchId(MatchId id) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findInProgressByPlayer(PlayerId p) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findWaitingByPlayer(PlayerId p) {

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
      public Optional<Cup> findById(CupId id) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findByMatchId(MatchId id) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findInProgressByPlayer(PlayerId p) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findWaitingByPlayer(PlayerId p) {

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
    final var checker = new PlayerAvailabilityChecker(matchQueryRepository, leagueQueryRepository,
        cupQueryRepository, noBotRegistry);
    final var rawHandler = new StartMatchCommandHandler(matchResolver, matchRepository,
        matchEventNotifier, checker);
    handler = pipeline.wrap(rawHandler)::handle;

    playerOne = PlayerId.generate();
    playerTwo = PlayerId.generate();
    match = Match.createReady(playerOne, playerTwo, MatchRules.fromGamesToPlay(GamesToPlay.of(5)));
    store.put(match.getId(), match);
    versions.put(match.getId(), new AtomicLong(0));
  }

  @RepeatedTest(50)
  @DisplayName("dos start simultáneos siempre producen estado IN_PROGRESS consistente")
  void concurrentStartAlwaysProducesConsistentState() throws InterruptedException {

    final var matchId = match.getId().value().toString();
    final var latch = new CountDownLatch(1);

    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      executor.submit(() -> {
        awaitLatch(latch);
        handler.handle(new StartMatchCommand(matchId, playerOne.value().toString()));
      });
      executor.submit(() -> {
        awaitLatch(latch);
        handler.handle(new StartMatchCommand(matchId, playerTwo.value().toString()));
      });

      latch.countDown();
      executor.shutdown();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    final var finalMatch = store.get(match.getId());
    assertThat(finalMatch.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
    assertThat(finalMatch.isReadyPlayerOne()).isTrue();
    assertThat(finalMatch.isReadyPlayerTwo()).isTrue();
    assertThat(finalMatch.getCurrentTurn()).isNotNull();
  }

  @RepeatedTest(50)
  @DisplayName("GAME_STARTED se emite exactamente una vez")
  void gameStartedEmittedExactlyOnce() throws InterruptedException {

    final var matchId = match.getId().value().toString();
    final var latch = new CountDownLatch(1);

    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      executor.submit(() -> {
        awaitLatch(latch);
        handler.handle(new StartMatchCommand(matchId, playerOne.value().toString()));
      });
      executor.submit(() -> {
        awaitLatch(latch);
        handler.handle(new StartMatchCommand(matchId, playerTwo.value().toString()));
      });

      latch.countDown();
      executor.shutdown();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    final var gameStartedCount = publishedEvents.stream()
        .filter(e -> "GAME_STARTED".equals(e.getEventType())).count();

    assertThat(gameStartedCount).isEqualTo(1);
  }

  @Test
  @DisplayName("llamadas repetidas después de inicio son idempotentes sin eventos extra")
  void repeatedCallsAfterStartAreIdempotent() {

    final var matchId = match.getId().value().toString();

    handler.handle(new StartMatchCommand(matchId, playerOne.value().toString()));
    handler.handle(new StartMatchCommand(matchId, playerTwo.value().toString()));

    final var eventsAfterStart = publishedEvents.size();

    handler.handle(new StartMatchCommand(matchId, playerOne.value().toString()));
    handler.handle(new StartMatchCommand(matchId, playerTwo.value().toString()));

    assertThat(publishedEvents.size()).isEqualTo(eventsAfterStart);
    assertThat(store.get(match.getId()).getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
  }

}
