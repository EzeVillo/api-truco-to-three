package com.villo.truco.application.ports;

import java.util.function.Supplier;

public interface AggregateLockManager<K> {

  <T> T executeWithLock(K id, Supplier<T> action);

  default void executeWithLock(K id, Runnable action) {

    executeWithLock(id, () -> {
      action.run();
      return null;
    });
  }

}
