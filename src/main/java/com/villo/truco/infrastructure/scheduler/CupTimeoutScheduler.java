package com.villo.truco.infrastructure.scheduler;

import com.villo.truco.application.ports.in.TimeoutIdleCupsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CupTimeoutScheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(CupTimeoutScheduler.class);

  private final TimeoutIdleCupsUseCase timeoutIdleCupsUseCase;

  public CupTimeoutScheduler(final TimeoutIdleCupsUseCase timeoutIdleCupsUseCase) {

    this.timeoutIdleCupsUseCase = timeoutIdleCupsUseCase;
  }

  @Scheduled(fixedDelayString = "${truco.cup.timeout-check-interval-ms:60000}")
  public void checkIdleCups() {

    LOGGER.debug("Checking for idle cups...");
    this.timeoutIdleCupsUseCase.handle();
  }

}
