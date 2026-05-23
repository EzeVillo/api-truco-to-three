package com.villo.truco.social.application.eventhandlers;

import com.villo.truco.application.eventhandlers.AbstractTimeoutEventHandler;
import com.villo.truco.application.eventhandlers.TimeoutActionDispatcher;
import com.villo.truco.application.ports.out.ResourceInvitationDomainEventHandler;
import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutScheduler;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationAcceptedEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationCancelledEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationDeclinedEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationDomainEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationExpiredEvent;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationReceivedEvent;

public class ResourceInvitationTimeoutEventHandler extends AbstractTimeoutEventHandler implements
    ResourceInvitationDomainEventHandler<ResourceInvitationDomainEvent> {

  public ResourceInvitationTimeoutEventHandler(final TimeoutScheduler timeoutScheduler,
      final TimeoutActionDispatcher dispatcher) {

    super(timeoutScheduler, dispatcher);
  }

  @Override
  public Class<ResourceInvitationDomainEvent> eventType() {

    return ResourceInvitationDomainEvent.class;
  }

  @Override
  public void handle(final ResourceInvitationDomainEvent event) {

    final var invitationId = event.getInvitationId().value().toString();

    if (event instanceof ResourceInvitationAcceptedEvent
        || event instanceof ResourceInvitationDeclinedEvent
        || event instanceof ResourceInvitationCancelledEvent
        || event instanceof ResourceInvitationExpiredEvent) {
      cancelTimeout(EntityType.RESOURCE_INVITATION, invitationId);
    } else if (event instanceof ResourceInvitationReceivedEvent) {
      scheduleTimeout(EntityType.RESOURCE_INVITATION, invitationId, event.getExpiresAt());
    }
  }

}
