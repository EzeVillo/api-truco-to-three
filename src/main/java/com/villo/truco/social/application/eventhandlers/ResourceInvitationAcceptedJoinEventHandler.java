package com.villo.truco.social.application.eventhandlers;

import com.villo.truco.application.ports.out.DomainEventHandler;
import com.villo.truco.application.usecases.commands.JoinTargetDispatcher;
import com.villo.truco.domain.shared.valueobjects.JoinTargetType;
import com.villo.truco.social.domain.model.invitation.events.ResourceInvitationAcceptedEvent;
import java.util.Objects;

public final class ResourceInvitationAcceptedJoinEventHandler implements
    DomainEventHandler<ResourceInvitationAcceptedEvent> {

  private final JoinTargetDispatcher joinTargetDispatcher;

  public ResourceInvitationAcceptedJoinEventHandler(
      final JoinTargetDispatcher joinTargetDispatcher) {

    this.joinTargetDispatcher = Objects.requireNonNull(joinTargetDispatcher);
  }

  @Override
  public Class<ResourceInvitationAcceptedEvent> eventType() {

    return ResourceInvitationAcceptedEvent.class;
  }

  @Override
  public void handle(final ResourceInvitationAcceptedEvent event) {

    this.joinTargetDispatcher.joinByTarget(event.getRecipientId(),
        JoinTargetType.valueOf(event.getTargetType().name()), event.getTargetId());
  }

}
