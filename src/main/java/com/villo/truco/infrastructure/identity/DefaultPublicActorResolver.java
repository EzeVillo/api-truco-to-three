package com.villo.truco.infrastructure.identity;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class DefaultPublicActorResolver implements PublicActorResolver {

  private static final int GUEST_SUFFIX_LENGTH = 8;
  private final UserQueryRepository userQueryRepository;
  private final BotRegistry botRegistry;

  public DefaultPublicActorResolver(final UserQueryRepository userQueryRepository,
      final BotRegistry botRegistry) {

    this.userQueryRepository = Objects.requireNonNull(userQueryRepository);
    this.botRegistry = Objects.requireNonNull(botRegistry);
  }

  private static String guestDisplayName(final PlayerId playerId) {

    final var rawId = playerId.value().toString();
    final var suffix = rawId.substring(0, Math.min(GUEST_SUFFIX_LENGTH, rawId.length()));
    return "Invitado-" + suffix;
  }

  @Override
  public String resolve(final PlayerId playerId) {

    return this.resolveAll(Set.of(playerId)).get(playerId);
  }

  @Override
  public Map<PlayerId, String> resolveAll(final Collection<PlayerId> playerIds) {

    final var uniquePlayerIds = Set.copyOf(playerIds);
    if (uniquePlayerIds.isEmpty()) {
      return Map.of();
    }

    final var usernamesById = this.userQueryRepository.findUsernamesByIds(uniquePlayerIds);
    final var resolvedActors = new LinkedHashMap<PlayerId, String>();

    for (final var playerId : uniquePlayerIds) {
      final var username = usernamesById.get(playerId);
      if (username != null) {
        resolvedActors.put(playerId, username);
      } else {
        final var displayName = this.botRegistry.getProfile(playerId).map(BotProfile::displayName)
            .orElseGet(() -> guestDisplayName(playerId));
        resolvedActors.put(playerId, displayName);
      }
    }

    return resolvedActors;
  }

}
