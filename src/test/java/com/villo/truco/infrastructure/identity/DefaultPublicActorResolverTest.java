package com.villo.truco.infrastructure.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DefaultPublicActorResolver")
class DefaultPublicActorResolverTest {

  private static UserQueryRepository userQueryRepositoryWith(
      final Map<PlayerId, String> usernamesById) {

    return new UserQueryRepository() {
      @Override
      public Map<PlayerId, String> findUsernamesByIds(final Set<PlayerId> playerIds) {

        final var result = new java.util.LinkedHashMap<PlayerId, String>();
        for (final var playerId : playerIds) {
          final var username = usernamesById.get(playerId);
          if (username != null) {
            result.put(playerId, username);
          }
        }
        return result;
      }

      @Override
      public Optional<PlayerId> findUserIdByUsername(final String username) {

        return usernamesById.entrySet().stream().filter(entry -> entry.getValue().equals(username))
            .map(Map.Entry::getKey).findFirst();
      }
    };
  }

  private static BotRegistry botRegistryWith(final BotProfile profile) {

    return new BotRegistry() {
      @Override
      public boolean isBot(final PlayerId playerId) {

        return profile.playerId().equals(playerId);
      }

      @Override
      public Optional<BotProfile> getProfile(final PlayerId playerId) {

        return profile.playerId().equals(playerId) ? Optional.of(profile) : Optional.empty();
      }

      @Override
      public List<BotProfile> getAll() {

        return List.of(profile);
      }

      @Override
      public void register(final BotProfile botProfile) {

      }
    };
  }

  private static BotRegistry emptyBotRegistry() {

    return new BotRegistry() {
      @Override
      public boolean isBot(final PlayerId playerId) {

        return false;
      }

      @Override
      public Optional<BotProfile> getProfile(final PlayerId playerId) {

        return Optional.empty();
      }

      @Override
      public List<BotProfile> getAll() {

        return List.of();
      }

      @Override
      public void register(final BotProfile profile) {

      }
    };
  }

  @Test
  @DisplayName("usa username para usuarios registrados")
  void resolvesRegisteredUserToUsername() {

    final var playerId = PlayerId.generate();
    final var resolver = new DefaultPublicActorResolver(
        userQueryRepositoryWith(Map.of(playerId, "juancho")), emptyBotRegistry());

    assertThat(resolver.resolve(playerId)).isEqualTo("juancho");
  }

  @Test
  @DisplayName("usa displayName para bots")
  void resolvesBotToDisplayName() {

    final var botId = PlayerId.generate();
    final var bot = new BotProfile(botId, "El Mentiroso", new BotPersonality(90, 20, 70, 50, 30));
    final var resolver = new DefaultPublicActorResolver(userQueryRepositoryWith(Map.of()),
        botRegistryWith(bot));

    assertThat(resolver.resolve(botId)).isEqualTo("El Mentiroso");
  }

  @Test
  @DisplayName("usa alias de invitado para players sin cuenta ni bot")
  void resolvesGuestToSyntheticDisplayName() {

    final var guestId = PlayerId.of("12345678-1234-1234-1234-1234567890ab");
    final var resolver = new DefaultPublicActorResolver(userQueryRepositoryWith(Map.of()),
        emptyBotRegistry());

    assertThat(resolver.resolve(guestId)).isEqualTo("Invitado-12345678");
  }

  @Test
  @DisplayName("prioriza username cuando existe usuario y no cae en fallback")
  void doesNotFallbackWhenUserExists() {

    final var playerId = PlayerId.of("aaaaaaaa-1234-1234-1234-1234567890ab");
    final var resolver = new DefaultPublicActorResolver(
        userQueryRepositoryWith(Map.of(playerId, "martina")), emptyBotRegistry());

    assertThat(resolver.resolve(playerId)).isEqualTo("martina");
  }

  @Test
  @DisplayName("resuelve varios actores en batch conservando usernames, bots e invitados")
  void resolvesAllActorsInBatch() {

    final var registeredPlayer = PlayerId.generate();
    final var botId = PlayerId.generate();
    final var guestId = PlayerId.of("bbbbbbbb-1234-1234-1234-1234567890ab");
    final var bot = new BotProfile(botId, "El Pescador", new BotPersonality(30, 40, 50, 60, 70));

    final var resolver = new DefaultPublicActorResolver(
        userQueryRepositoryWith(Map.of(registeredPlayer, "flor")), botRegistryWith(bot));

    final var resolvedActors = resolver.resolveAll(
        List.of(registeredPlayer, botId, guestId, registeredPlayer));

    assertThat(resolvedActors).containsEntry(registeredPlayer, "flor")
        .containsEntry(botId, "El Pescador").containsEntry(guestId, "Invitado-bbbbbbbb");
  }

}
