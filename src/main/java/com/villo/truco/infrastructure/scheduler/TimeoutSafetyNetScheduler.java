package com.villo.truco.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TimeoutSafetyNetScheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutSafetyNetScheduler.class);

  private final TimeoutReconciliationRunner reconciliationRunner;

  public TimeoutSafetyNetScheduler(final TimeoutReconciliationRunner reconciliationRunner) {

    this.reconciliationRunner = reconciliationRunner;
  }

  @Scheduled(fixedDelayString = "${truco.timeout.safety-net-interval:300000}")
  public void run() {

    LOGGER.debug("Ejecutando red de seguridad de timeouts...");
    reconciliationRunner.reconcile("safety-net");
  }

}
