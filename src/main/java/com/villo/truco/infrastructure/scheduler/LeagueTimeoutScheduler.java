package com.villo.truco.infrastructure.scheduler;

import com.villo.truco.application.ports.in.TimeoutIdleLeaguesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LeagueTimeoutScheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(LeagueTimeoutScheduler.class);

  private final TimeoutIdleLeaguesUseCase timeoutIdleLeaguesUseCase;

  public LeagueTimeoutScheduler(final TimeoutIdleLeaguesUseCase timeoutIdleLeaguesUseCase) {

    this.timeoutIdleLeaguesUseCase = timeoutIdleLeaguesUseCase;
  }

  @Scheduled(fixedDelayString = "${truco.league.timeout-check-interval-ms:60000}")
  public void checkIdleLeagues() {

    LOGGER.debug("Checking for idle leagues...");
    this.timeoutIdleLeaguesUseCase.handle();
  }

}
