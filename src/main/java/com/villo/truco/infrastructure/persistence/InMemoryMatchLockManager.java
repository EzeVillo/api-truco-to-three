package com.villo.truco.infrastructure.persistence;

import com.villo.truco.application.ports.MatchLockManager;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public final class InMemoryMatchLockManager implements MatchLockManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryMatchLockManager.class);

  private final ConcurrentHashMap<MatchId, LockEntry> locks = new ConcurrentHashMap<>();

  @Override
  public <T> T executeWithLock(final MatchId matchId, final Supplier<T> action) {

    final var entry = this.acquireEntry(matchId);
    LOGGER.debug("Trying to acquire lock: matchId={}, refs={}, queueLength={}", matchId,
        entry.references(), entry.queueLength());
    entry.lock().lock();
    try {
      LOGGER.debug("Lock acquired: matchId={}", matchId);
      return action.get();
    } finally {
      entry.lock().unlock();
      this.releaseEntry(matchId, entry);
      LOGGER.debug("Lock released: matchId={}", matchId);
    }
  }

  private LockEntry acquireEntry(final MatchId matchId) {

    return this.locks.compute(matchId, (id, existing) -> {
      final var entry = existing != null ? existing : new LockEntry();
      entry.incrementRefs();
      return entry;
    });
  }

  private void releaseEntry(final MatchId matchId, final LockEntry entry) {

    this.locks.computeIfPresent(matchId, (id, current) -> {
      if (current != entry) {
        return current;
      }

      final var refs = entry.decrementRefs();
      return refs == 0 ? null : current;
    });
  }

  private static final class LockEntry {

    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicInteger refs = new AtomicInteger(0);

    private ReentrantLock lock() {

      return this.lock;
    }

    private void incrementRefs() {

      this.refs.incrementAndGet();
    }

    private int decrementRefs() {

      return this.refs.decrementAndGet();
    }

    private int references() {

      return this.refs.get();
    }

    private int queueLength() {

      return this.lock.getQueueLength();
    }

  }

}
