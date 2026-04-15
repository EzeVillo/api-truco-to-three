package com.villo.truco.social.application.services;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.exceptions.ResourceInvitationAlreadyExistsException;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;
import com.villo.truco.social.domain.ports.ResourceInvitationQueryRepository;
import java.util.Objects;

public final class ResourceInvitationPolicy {

  private final ResourceInvitationQueryRepository resourceInvitationQueryRepository;

  public ResourceInvitationPolicy(
      final ResourceInvitationQueryRepository resourceInvitationQueryRepository) {

    this.resourceInvitationQueryRepository = Objects.requireNonNull(
        resourceInvitationQueryRepository);
  }

  public void ensureNoDuplicatePending(final PlayerId senderId, final PlayerId recipientId,
      final ResourceInvitationTargetType targetType, final String targetId) {

    if (this.resourceInvitationQueryRepository.existsPendingBySenderAndRecipientAndTarget(senderId,
        recipientId, targetType, targetId)) {
      throw new ResourceInvitationAlreadyExistsException();
    }
  }

}
