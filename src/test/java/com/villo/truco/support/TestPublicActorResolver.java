package com.villo.truco.support;

import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

public final class TestPublicActorResolver {

  private TestPublicActorResolver() {

  }

  public static PublicActorResolver guestStyle() {

    return TestPublicActorResolver::displayName;
  }

  public static String displayName(final PlayerId playerId) {

    final var rawId = playerId.value().toString();
    return "Invitado-" + rawId.substring(0, Math.min(8, rawId.length()));
  }

}
