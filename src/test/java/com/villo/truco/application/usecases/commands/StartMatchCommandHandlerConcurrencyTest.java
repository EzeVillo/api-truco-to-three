package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.commands.StartMatchCommand;
import com.villo.truco.application.ports.MatchLockManager;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.infrastructure.persistence.InMemoryMatchLockManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

@DisplayName("StartMatchCommandHandler — concurrency")
class StartMatchCommandHandlerConcurrencyTest {

  private final Map<MatchId, Match> store = new ConcurrentHashMap<>();
  private final List<DomainEventBase> publishedEvents = Collections.synchronizedList(
      new ArrayList<>());

  private final MatchRepository matchRepository = match -> store.put(match.getId(), match);
  private final MatchQueryRepository matchQueryRepository = id -> Optional.ofNullable(
      store.get(id));
  private final MatchEventNotifier matchEventNotifier = (matchId, p1, p2, events) -> publishedEvents.addAll(
      events);
  private final MatchLockManager matchLockManager = new InMemoryMatchLockManager();

  private MatchResolver matchResolver;
  private StartMatchCommandHandler handler;

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
    publishedEvents.clear();

    matchResolver = new MatchResolver(matchQueryRepository);
    handler = new StartMatchCommandHandler(matchResolver, matchRepository, matchEventNotifier,
        matchLockManager);

    playerOne = PlayerId.generate();
    playerTwo = PlayerId.generate();
    match = Match.create(playerOne, playerTwo);
    match.join(match.getInviteCode());
    store.put(match.getId(), match);
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

    assertThat(match.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
    assertThat(match.isReadyPlayerOne()).isTrue();
    assertThat(match.isReadyPlayerTwo()).isTrue();
    assertThat(match.getCurrentTurn()).isNotNull();
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
    assertThat(match.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
  }

  @Test
  @DisplayName("el segundo start espera hasta que el primero libera el lock")
  void secondStartWaitsUntilFirstReleasesLock() throws Exception {

    final var matchId = match.getId().value().toString();
    final var firstInsideCriticalSection = new CountDownLatch(1);
    final var releaseFirst = new CountDownLatch(1);
    final var firstInvocation = new AtomicBoolean(true);

    final MatchEventNotifier blockingNotifier = (id, p1, p2, events) -> {
      publishedEvents.addAll(events);
      if (firstInvocation.compareAndSet(true, false)) {
        firstInsideCriticalSection.countDown();
        awaitLatch(releaseFirst);
      }
    };

    final var blockingHandler = new StartMatchCommandHandler(matchResolver, matchRepository,
        blockingNotifier, matchLockManager);

    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      final Future<MatchId> first = executor.submit(() -> blockingHandler.handle(
          new StartMatchCommand(matchId, playerOne.value().toString())));

      assertThat(firstInsideCriticalSection.await(1, TimeUnit.SECONDS)).isTrue();

      final Future<MatchId> second = executor.submit(() -> blockingHandler.handle(
          new StartMatchCommand(matchId, playerTwo.value().toString())));

      Thread.sleep(200);
      assertThat(second.isDone()).isFalse();

      releaseFirst.countDown();

      assertThat(first.get(2, TimeUnit.SECONDS)).isEqualTo(match.getId());
      assertThat(second.get(2, TimeUnit.SECONDS)).isEqualTo(match.getId());
    }

    assertThat(match.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
    assertThat(match.isReadyPlayerOne()).isTrue();
    assertThat(match.isReadyPlayerTwo()).isTrue();
  }

}
