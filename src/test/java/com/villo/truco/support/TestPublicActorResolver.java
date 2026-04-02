package com.villo.truco.support;

import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TestPublicActorResolver {

  private TestPublicActorResolver() {

  }

  public static PublicActorResolver guestStyle() {

    return new PublicActorResolver() {
      @Override
      public String resolve(final PlayerId playerId) {

        return displayName(playerId);
      }

      @Override
      public Map<PlayerId, String> resolveAll(final Collection<PlayerId> playerIds) {

        final var resolved = new LinkedHashMap<PlayerId, String>();
        for (final var playerId : playerIds) {
          resolved.putIfAbsent(playerId, displayName(playerId));
        }
        return resolved;
      }
    };
  }

  public static String displayName(final PlayerId playerId) {

    final var rawId = playerId.value().toString();
    return "Invitado-" + rawId.substring(0, Math.min(8, rawId.length()));
  }

}
