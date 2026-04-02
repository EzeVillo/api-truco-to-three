package com.villo.truco.infrastructure.identity;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.auth.domain.ports.UserRepository;
import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public final class DefaultPublicActorResolver implements PublicActorResolver {

  private static final int GUEST_SUFFIX_LENGTH = 8;
  private final UserRepository userRepository;
  private final BotRegistry botRegistry;

  public DefaultPublicActorResolver(final UserRepository userRepository,
      final BotRegistry botRegistry) {

    this.userRepository = Objects.requireNonNull(userRepository);
    this.botRegistry = Objects.requireNonNull(botRegistry);
  }

  private static String guestDisplayName(final PlayerId playerId) {

    final var rawId = playerId.value().toString();
    final var suffix = rawId.substring(0, Math.min(GUEST_SUFFIX_LENGTH, rawId.length()));
    return "Invitado-" + suffix;
  }

  @Override
  public String resolve(final PlayerId playerId) {

    return this.userRepository.findById(playerId).map(user -> user.username().value())
        .or(() -> this.botRegistry.getProfile(playerId).map(BotProfile::displayName))
        .orElseGet(() -> guestDisplayName(playerId));
  }

}
