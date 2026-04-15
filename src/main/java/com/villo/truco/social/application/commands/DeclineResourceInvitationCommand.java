package com.villo.truco.social.application.commands;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationId;
import java.util.Objects;

public record DeclineResourceInvitationCommand(ResourceInvitationId invitationId,
                                               PlayerId actorId) {

  public DeclineResourceInvitationCommand {

    Objects.requireNonNull(invitationId);
    Objects.requireNonNull(actorId);
  }

  public DeclineResourceInvitationCommand(final String invitationId, final String actorId) {

    this(ResourceInvitationId.of(invitationId), PlayerId.of(actorId));
  }

}
