package com.villo.truco.infrastructure.scheduler;

import com.villo.truco.application.ports.in.TimeoutIdleMatchesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MatchTimeoutScheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(MatchTimeoutScheduler.class);

  private final TimeoutIdleMatchesUseCase timeoutIdleMatchesUseCase;

  public MatchTimeoutScheduler(final TimeoutIdleMatchesUseCase timeoutIdleMatchesUseCase) {

    this.timeoutIdleMatchesUseCase = timeoutIdleMatchesUseCase;
  }

  @Scheduled(fixedDelayString = "${truco.match.timeout-check-interval-ms:30000}")
  public void checkIdleMatches() {

    LOGGER.debug("Checking for idle matches...");
    this.timeoutIdleMatchesUseCase.handle();
  }

}
