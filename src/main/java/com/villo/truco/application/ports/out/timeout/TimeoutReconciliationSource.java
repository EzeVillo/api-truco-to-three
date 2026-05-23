package com.villo.truco.application.ports.out.timeout;

import java.time.Instant;
import java.util.stream.Stream;

public interface TimeoutReconciliationSource {

  Stream<TimeoutEntry> activeWithDeadline();

  record TimeoutEntry(TimeoutKey key, Instant deadline, Runnable action) {

  }

}
