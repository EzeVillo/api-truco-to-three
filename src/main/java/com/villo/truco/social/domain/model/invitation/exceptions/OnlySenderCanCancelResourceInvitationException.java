package com.villo.truco.social.domain.model.invitation.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class OnlySenderCanCancelResourceInvitationException extends DomainException {

  public OnlySenderCanCancelResourceInvitationException() {

    super("Only the sender can cancel the resource invitation");
  }

}
