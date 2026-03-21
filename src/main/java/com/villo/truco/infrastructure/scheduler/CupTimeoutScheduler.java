package com.villo.truco.infrastructure.scheduler;

import com.villo.truco.application.ports.in.TimeoutIdleCupsUseCase;
import com.villo.truco.infrastructure.actuator.health.SchedulerHeartbeatRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CupTimeoutScheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(CupTimeoutScheduler.class);

  private final TimeoutIdleCupsUseCase timeoutIdleCupsUseCase;
  private final SchedulerHeartbeatRegistry schedulerHeartbeatRegistry;

  public CupTimeoutScheduler(final TimeoutIdleCupsUseCase timeoutIdleCupsUseCase,
      final SchedulerHeartbeatRegistry schedulerHeartbeatRegistry) {

    this.timeoutIdleCupsUseCase = timeoutIdleCupsUseCase;
    this.schedulerHeartbeatRegistry = schedulerHeartbeatRegistry;
  }

  @Scheduled(fixedDelayString = "${truco.cup.timeout-check-interval-ms:60000}")
  public void checkIdleCups() {

    LOGGER.debug("Checking for idle cups...");
    this.timeoutIdleCupsUseCase.handle();
    this.schedulerHeartbeatRegistry.recordSuccessfulRun("cup-timeout");
  }

}
