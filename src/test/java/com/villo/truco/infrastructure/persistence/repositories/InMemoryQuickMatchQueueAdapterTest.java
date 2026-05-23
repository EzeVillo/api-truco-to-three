package com.villo.truco.infrastructure.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.quickmatch.QuickMatchTicket;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InMemoryQuickMatchQueueAdapter")
class InMemoryQuickMatchQueueAdapterTest {

  private static final GamesToPlay THREE = GamesToPlay.of(3);
  private InMemoryQuickMatchQueueAdapter adapter;

  @BeforeEach
  void setUp() {

    adapter = new InMemoryQuickMatchQueueAdapter();
  }

  private QuickMatchTicket ticket(final PlayerId playerId) {

    return new QuickMatchTicket(playerId, THREE, Instant.now(), null);
  }

  @Test
  @DisplayName("enqueue then isPlayerQueued returns true")
  void enqueueThenIsQueued() {

    final var player = PlayerId.generate();
    adapter.enqueue(ticket(player));

    assertThat(adapter.isPlayerQueued(player)).isTrue();
  }

  @Test
  @DisplayName("FIFO ordering: tryMatchOpponent returns earliest ticket")
  void fifoOrdering() throws InterruptedException {

    final var playerA = PlayerId.generate();
    final var playerB = PlayerId.generate();
    final var playerC = PlayerId.generate();

    adapter.enqueue(new QuickMatchTicket(playerA, THREE, Instant.now(), null));
    Thread.sleep(5);
    adapter.enqueue(new QuickMatchTicket(playerB, THREE, Instant.now(), null));

    final var opponent = adapter.tryMatchOpponent(playerC, THREE);

    assertThat(opponent).isPresent();
    assertThat(opponent.get().playerId()).isEqualTo(playerA);
  }

  @Test
  @DisplayName("tryMatchOpponent does not match the enqueuingPlayer's own ticket")
  void doesNotMatchSelf() {

    final var player = PlayerId.generate();
    adapter.enqueue(ticket(player));

    final var opponent = adapter.tryMatchOpponent(player, THREE);

    assertThat(opponent).isEmpty();
    assertThat(adapter.isPlayerQueued(player)).isTrue();
  }

  @Test
  @DisplayName("tryDequeue removes from all indexes")
  void tryDequeueRemovesCompletely() {

    final var player = PlayerId.generate();
    adapter.enqueue(ticket(player));

    final var result = adapter.tryDequeue(player);

    assertThat(result).isPresent();
    assertThat(adapter.isPlayerQueued(player)).isFalse();
  }

  @Test
  @DisplayName("tryDequeueBySessionId removes by sessionId")
  void tryDequeueBySessionId() {

    final var player = PlayerId.generate();
    final var sessionId = "ws-session-42";
    adapter.enqueue(new QuickMatchTicket(player, THREE, Instant.now(), sessionId));

    final var result = adapter.tryDequeueBySessionId(sessionId);

    assertThat(result).isPresent();
    assertThat(result.get().playerId()).isEqualTo(player);
    assertThat(adapter.isPlayerQueued(player)).isFalse();
  }

  @Test
  @DisplayName("findByPlayer returns ticket without removing it")
  void findByPlayerDoesNotRemove() {

    final var player = PlayerId.generate();
    adapter.enqueue(ticket(player));

    final var found = adapter.findByPlayer(player);

    assertThat(found).isPresent();
    assertThat(adapter.isPlayerQueued(player)).isTrue();
  }

  @Test
  @DisplayName("concurrent enqueue + tryMatchOpponent: exactly one match occurs")
  void concurrentExactlyOneMatch() throws InterruptedException {

    final var playerA = PlayerId.generate();
    final var playerB = PlayerId.generate();
    adapter.enqueue(ticket(playerA));

    final int threadCount = 10;
    final var matchCount = new AtomicInteger(0);
    final var latch = new CountDownLatch(threadCount);
    final var executor = Executors.newFixedThreadPool(threadCount);

    for (int i = 0; i < threadCount; i++) {
      executor.submit(() -> {
        final var result = adapter.tryMatchOpponent(playerB, THREE);
        if (result.isPresent()) {
          matchCount.incrementAndGet();
        }
        latch.countDown();
      });
    }

    latch.await();
    executor.shutdown();

    assertThat(matchCount.get()).isEqualTo(1);
  }

}
