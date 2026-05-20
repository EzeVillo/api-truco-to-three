package com.villo.truco.infrastructure.scheduler;

import com.villo.truco.application.usecases.commands.ExpireDueRematchSessionsCommandHandler;
import com.villo.truco.infrastructure.actuator.health.SchedulerHeartbeatRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RematchSessionExpirationScheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      RematchSessionExpirationScheduler.class);

  private final ExpireDueRematchSessionsCommandHandler handler;
  private final SchedulerHeartbeatRegistry schedulerHeartbeatRegistry;

  public RematchSessionExpirationScheduler(final ExpireDueRematchSessionsCommandHandler handler,
      final SchedulerHeartbeatRegistry schedulerHeartbeatRegistry) {

    this.handler = handler;
    this.schedulerHeartbeatRegistry = schedulerHeartbeatRegistry;
  }

  @Scheduled(fixedDelayString = "${truco.rematch.scheduler-delay:PT10S}")
  public void expire() {

    LOGGER.debug("Checking for expired rematch sessions...");
    this.handler.expireAll();
    this.schedulerHeartbeatRegistry.recordSuccessfulRun("rematch-session-expiration");
  }

}
