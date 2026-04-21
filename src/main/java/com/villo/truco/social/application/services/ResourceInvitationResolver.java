package com.villo.truco.social.application.services;

import com.villo.truco.social.application.exceptions.ResourceInvitationNotFoundException;
import com.villo.truco.social.domain.model.invitation.ResourceInvitation;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationId;
import com.villo.truco.social.domain.ports.ResourceInvitationQueryRepository;
import java.util.Objects;

public final class ResourceInvitationResolver {

  private final ResourceInvitationQueryRepository resourceInvitationQueryRepository;

  public ResourceInvitationResolver(
      final ResourceInvitationQueryRepository resourceInvitationQueryRepository) {

    this.resourceInvitationQueryRepository = Objects.requireNonNull(
        resourceInvitationQueryRepository);
  }

  public ResourceInvitation resolve(final ResourceInvitationId invitationId) {

    return this.resourceInvitationQueryRepository.findById(invitationId)
        .orElseThrow(() -> new ResourceInvitationNotFoundException(invitationId));
  }

}
