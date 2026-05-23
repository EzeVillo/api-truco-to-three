package com.villo.truco.application.ports.out.timeout;

import java.time.Instant;

public interface TimeoutScheduler {

  void schedule(TimeoutKey key, Instant deadline, Runnable action);

  void cancel(TimeoutKey key);

  boolean isPending(TimeoutKey key);

}
