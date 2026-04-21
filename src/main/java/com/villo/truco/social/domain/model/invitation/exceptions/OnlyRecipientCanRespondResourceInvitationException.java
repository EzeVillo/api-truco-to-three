package com.villo.truco.social.domain.model.invitation.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class OnlyRecipientCanRespondResourceInvitationException extends DomainException {

  public OnlyRecipientCanRespondResourceInvitationException() {

    super("Only the recipient can respond to the resource invitation");
  }

}
