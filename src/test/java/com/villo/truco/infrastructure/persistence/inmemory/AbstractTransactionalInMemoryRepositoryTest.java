package com.villo.truco.infrastructure.persistence.inmemory;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.support.TestTransactionRunner;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AbstractTransactionalInMemoryRepository")
class AbstractTransactionalInMemoryRepositoryTest {

  private static void await(final CountDownLatch latch) {

    try {
      assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Latch interrupted", e);
    }
  }

  private static void awaitUntil(final Check check) {

    final var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (System.nanoTime() < deadline) {
      if (check.test()) {
        return;
      }
      Thread.yield();
    }
    throw new AssertionError("Condition not met before timeout");
  }

  @Test
  @DisplayName("libera entradas del registry despues de operaciones secuenciales")
  void releasesRegistryEntriesAfterSequentialWrites() {

    final var repository = new TestRepository();

    repository.write("chat:1");
    assertThat(repository.registeredLocks()).isZero();

    repository.write("chat:2");
    assertThat(repository.registeredLocks()).isZero();
  }

  @Test
  @DisplayName("mantiene una sola entrada mientras hay waiters del mismo recurso")
  void keepsSingleEntryWhileSameResourceHasWaiters() throws InterruptedException {

    final var repository = new TestRepository();
    final var firstInside = new CountDownLatch(1);
    final var releaseFirst = new CountDownLatch(1);
    final var secondFinished = new CountDownLatch(1);

    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      executor.submit(() -> {
        repository.write("chat:1", () -> {
          firstInside.countDown();
          await(releaseFirst);
        });
      });

      await(firstInside);
      assertThat(repository.registeredLocks()).isEqualTo(1);

      executor.submit(() -> {
        repository.write("chat:1");
        secondFinished.countDown();
      });

      // The second write references the same lock entry while waiting on it.
      awaitUntil(() -> repository.registeredLocks() == 1);

      releaseFirst.countDown();
      assertThat(secondFinished.await(5, TimeUnit.SECONDS)).isTrue();
      executor.shutdown();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    assertThat(repository.registeredLocks()).isZero();
  }

  @Test
  @DisplayName("en transaccion mantiene locks hasta afterCompletion")
  void keepsLocksUntilTransactionCompletion() {

    final var repository = new TestRepository();

    TestTransactionRunner.inTransaction(() -> {
      repository.write("chat:1");
      assertThat(repository.registeredLocks()).isEqualTo(1);
    });

    assertThat(repository.registeredLocks()).isZero();
  }

  @FunctionalInterface
  private interface Check {

    boolean test();

  }

  private static final class TestRepository extends
      AbstractTransactionalInMemoryRepository<TestRepository.TestContext> {

    private void write(final String resource) {

      this.write(resource, () -> {
      });
    }

    private void write(final String resource, final Runnable action) {

      this.executeWrite(List.of(resource), context -> {
        action.run();
        return null;
      });
    }

    private int registeredLocks() {

      return this.registeredLockCount();
    }

    @Override
    protected TestContext newTransactionContext() {

      return new TestContext();
    }

    @Override
    protected void commit(final TestContext context) {

    }

    private static final class TestContext extends TransactionContext {

    }

  }

}
