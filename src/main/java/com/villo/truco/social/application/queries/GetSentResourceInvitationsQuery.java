package com.villo.truco.social.application.queries;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record GetSentResourceInvitationsQuery(PlayerId senderId) {

  public GetSentResourceInvitationsQuery {

    Objects.requireNonNull(senderId);
  }

  public GetSentResourceInvitationsQuery(final String senderId) {

    this(PlayerId.of(senderId));
  }

}
