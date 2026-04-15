package com.villo.truco.social.domain.model.invitation.valueobjects;

import com.villo.truco.domain.shared.exceptions.InvalidIdException;
import java.util.UUID;

public record ResourceInvitationId(UUID value) {

  public static ResourceInvitationId of(final String value) {

    try {
      return new ResourceInvitationId(UUID.fromString(value));
    } catch (final IllegalArgumentException ex) {
      throw new InvalidIdException(value);
    }
  }

  public static ResourceInvitationId generate() {

    return new ResourceInvitationId(UUID.randomUUID());
  }

}
