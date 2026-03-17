package com.villo.truco.infrastructure.persistence;

import com.villo.truco.application.ports.AggregateLockManager;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryAggregateLockManager<K> implements AggregateLockManager<K> {

  private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryAggregateLockManager.class);

  private final ConcurrentHashMap<K, LockEntry> locks = new ConcurrentHashMap<>();

  @Override
  public <T> T executeWithLock(final K id, final Supplier<T> action) {

    final var entry = this.acquireEntry(id);
    LOGGER.debug("Trying to acquire lock: id={}, refs={}, queueLength={}", id, entry.references(),
        entry.queueLength());
    entry.lock().lock();
    try {
      LOGGER.debug("Lock acquired: id={}", id);
      return action.get();
    } finally {
      entry.lock().unlock();
      this.releaseEntry(id, entry);
      LOGGER.debug("Lock released: id={}", id);
    }
  }

  private LockEntry acquireEntry(final K id) {

    return this.locks.compute(id, (key, existing) -> {
      final var entry = existing != null ? existing : new LockEntry();
      entry.incrementRefs();
      return entry;
    });
  }

  private void releaseEntry(final K id, final LockEntry entry) {

    this.locks.computeIfPresent(id, (key, current) -> {
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
