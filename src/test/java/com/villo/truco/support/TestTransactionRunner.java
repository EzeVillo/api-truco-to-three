package com.villo.truco.support;

import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class TestTransactionRunner {

  private TestTransactionRunner() {

  }

  public static void inTransaction(final Runnable action) {

    inTransaction(() -> {
      action.run();
      return null;
    });
  }

  public static <T> T inTransaction(final Supplier<T> action) {

    TransactionSynchronizationManager.initSynchronization();
    TransactionSynchronizationManager.setActualTransactionActive(true);
    try {
      final var result = action.get();
      complete(TransactionSynchronization.STATUS_COMMITTED);
      return result;
    } catch (final RuntimeException ex) {
      complete(TransactionSynchronization.STATUS_ROLLED_BACK);
      throw ex;
    } finally {
      TransactionSynchronizationManager.setActualTransactionActive(false);
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  private static void complete(final int status) {

    final var synchronizations = TransactionSynchronizationManager.getSynchronizations();
    for (final var synchronization : synchronizations) {
      synchronization.beforeCompletion();
    }
    for (int i = synchronizations.size() - 1; i >= 0; i--) {
      synchronizations.get(i).afterCompletion(status);
    }
  }

}
