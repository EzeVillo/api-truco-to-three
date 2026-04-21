package com.villo.truco.social.infrastructure.scheduler;

import com.villo.truco.infrastructure.actuator.health.SchedulerHeartbeatRegistry;
import com.villo.truco.social.application.ports.in.ExpirePendingResourceInvitationsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SocialInvitationExpirationScheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      SocialInvitationExpirationScheduler.class);

  private final ExpirePendingResourceInvitationsUseCase expirePendingResourceInvitationsUseCase;
  private final SchedulerHeartbeatRegistry schedulerHeartbeatRegistry;

  public SocialInvitationExpirationScheduler(
      final ExpirePendingResourceInvitationsUseCase expirePendingResourceInvitationsUseCase,
      final SchedulerHeartbeatRegistry schedulerHeartbeatRegistry) {

    this.expirePendingResourceInvitationsUseCase = expirePendingResourceInvitationsUseCase;
    this.schedulerHeartbeatRegistry = schedulerHeartbeatRegistry;
  }

  @Scheduled(fixedDelayString = "${truco.social.invitation-expiration-check-interval-ms:60000}")
  public void expirePendingInvitations() {

    LOGGER.debug("Checking for expired social invitations...");
    this.expirePendingResourceInvitationsUseCase.handle();
    this.schedulerHeartbeatRegistry.recordSuccessfulRun("social-invitation-expiration");
  }

}
