package com.villo.truco.infrastructure.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TimeoutSafetyNetScheduler — red de seguridad")
class TimeoutSafetyNetSchedulerTest {

  @Test
  @DisplayName("run() delega al reconciliador con fase 'safety-net'")
  void runDelegatesReconcileWithSafetyNetPhase() {

    final var reconciliationRunner = mock(TimeoutReconciliationRunner.class);
    final var scheduler = new TimeoutSafetyNetScheduler(reconciliationRunner);

    scheduler.run();

    verify(reconciliationRunner).reconcile("safety-net");
  }

}
