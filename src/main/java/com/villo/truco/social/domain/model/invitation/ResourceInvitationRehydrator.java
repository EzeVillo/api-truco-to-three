package com.villo.truco.social.domain.model.invitation;

public final class ResourceInvitationRehydrator {

  private ResourceInvitationRehydrator() {

  }

  public static ResourceInvitation rehydrate(final ResourceInvitationSnapshot snapshot) {

    return ResourceInvitation.reconstruct(snapshot.id(), snapshot.senderId(),
        snapshot.recipientId(), snapshot.targetType(), snapshot.targetId(), snapshot.status(),
        snapshot.expiresAt());
  }

}
