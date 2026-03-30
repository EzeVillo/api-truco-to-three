package com.villo.truco.infrastructure.persistence.inmemory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public abstract class AbstractTransactionalInMemoryRepository<T extends AbstractTransactionalInMemoryRepository.TransactionContext> {

  private final Object transactionResourceKey = new Object();
  private final ResourceLockRegistry lockRegistry = new ResourceLockRegistry();

  protected final <R> R executeWrite(final Collection<String> resources,
      final Function<T, R> action) {

    Objects.requireNonNull(resources);
    Objects.requireNonNull(action);

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      final var context = this.currentOrCreateTransactionContext();
      this.acquireLocks(context, resources);
      return action.apply(context);
    }

    final var context = this.newTransactionContext();
    this.acquireLocks(context, resources);
    try {
      final var result = action.apply(context);
      this.commit(context);
      return result;
    } catch (final RuntimeException ex) {
      this.rollback(context);
      throw ex;
    } finally {
      this.releaseLocks(context);
    }
  }

  protected final T currentTransactionContext() {

    @SuppressWarnings("unchecked") final var context = (T) TransactionSynchronizationManager.getResource(
        this.transactionResourceKey);
    return context;
  }

  protected abstract T newTransactionContext();

  protected abstract void commit(T context);

  protected void rollback(final T context) {

  }

  protected final int registeredLockCount() {

    return this.lockRegistry.size();
  }

  private T currentOrCreateTransactionContext() {

    final var existing = this.currentTransactionContext();
    if (existing != null) {
      return existing;
    }

    final var context = this.newTransactionContext();
    TransactionSynchronizationManager.bindResource(this.transactionResourceKey, context);
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCompletion(final int status) {

        try {
          if (status == STATUS_COMMITTED) {
            AbstractTransactionalInMemoryRepository.this.commit(context);
          } else {
            AbstractTransactionalInMemoryRepository.this.rollback(context);
          }
        } finally {
          if (TransactionSynchronizationManager.hasResource(transactionResourceKey)) {
            TransactionSynchronizationManager.unbindResource(transactionResourceKey);
          }
          AbstractTransactionalInMemoryRepository.this.releaseLocks(context);
        }
      }
    });
    return context;
  }

  private void acquireLocks(final T context, final Collection<String> resources) {

    final var sortedResources = resources.stream().filter(Objects::nonNull).distinct().sorted()
        .toList();
    for (final var resource : sortedResources) {
      if (context.hasLock(resource)) {
        continue;
      }
      final var handle = this.lockRegistry.acquire(resource);
      handle.acquire();
      context.rememberLock(resource, handle);
    }
  }

  private void releaseLocks(final T context) {

    final var acquiredLocks = List.copyOf(context.acquiredLocks().values());
    for (int i = acquiredLocks.size() - 1; i >= 0; i--) {
      this.lockRegistry.release(acquiredLocks.get(i));
    }
  }

  protected abstract static class TransactionContext {

    private final Map<String, LockHandle> acquiredLocks = new LinkedHashMap<>();

    final boolean hasLock(final String resource) {

      return this.acquiredLocks.containsKey(resource);
    }

    final void rememberLock(final String resource, final LockHandle lock) {

      this.acquiredLocks.put(resource, lock);
    }

    final Map<String, LockHandle> acquiredLocks() {

      return this.acquiredLocks;
    }

  }

  private static final class ResourceLockRegistry {

    private final ConcurrentMap<String, LockEntry> locksByResource = new ConcurrentHashMap<>();

    private LockHandle acquire(final String resource) {

      Objects.requireNonNull(resource);

      final var entry = this.locksByResource.compute(resource, (ignored, current) -> {
        if (current == null) {
          return LockEntry.create();
        }
        current.incrementHolders();
        return current;
      });
      return new LockHandle(resource, entry);
    }

    private void release(final LockHandle handle) {

      Objects.requireNonNull(handle);

      handle.reentrantLock().unlock();
      this.locksByResource.computeIfPresent(handle.resource(), (ignored, current) -> {
        if (current != handle.entry()) {
          return current;
        }
        return current.decrementHolders() == 0 ? null : current;
      });
    }

    private int size() {

      return this.locksByResource.size();
    }

  }

  private record LockHandle(String resource, LockEntry entry) {

    private void acquire() {

      this.entry.reentrantLock().lock();
    }

    private ReentrantLock reentrantLock() {

      return this.entry.reentrantLock();
    }

  }

  private static final class LockEntry {

    private final ReentrantLock lock;
    private int holders;

    private LockEntry(final ReentrantLock lock, final int holders) {

      this.lock = lock;
      this.holders = holders;
    }

    private static LockEntry create() {

      return new LockEntry(new ReentrantLock(), 1);
    }

    private ReentrantLock reentrantLock() {

      return this.lock;
    }

    private void incrementHolders() {

      this.holders++;
    }

    private int decrementHolders() {

      this.holders--;
      return this.holders;
    }

  }

}
