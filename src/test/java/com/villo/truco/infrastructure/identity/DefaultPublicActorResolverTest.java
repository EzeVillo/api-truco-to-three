package com.villo.truco.infrastructure.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.auth.domain.model.user.User;
import com.villo.truco.auth.domain.model.user.UserRehydrator;
import com.villo.truco.auth.domain.model.user.UserSnapshot;
import com.villo.truco.auth.domain.model.user.valueobjects.HashedPassword;
import com.villo.truco.auth.domain.model.user.valueobjects.Username;
import com.villo.truco.auth.domain.ports.UserRepository;
import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DefaultPublicActorResolver")
class DefaultPublicActorResolverTest {

  private static User rehydratedUser(final PlayerId playerId, final String username) {

    return UserRehydrator.rehydrate(
        new UserSnapshot(playerId, new Username(username), new HashedPassword("hashed")));
  }

  private static UserRepository userRepositoryWith(final Map<PlayerId, User> usersById) {

    return new UserRepository() {
      @Override
      public void saveEnsuringUsernameAvailable(final User user) {

      }

      @Override
      public Optional<User> findById(final PlayerId playerId) {

        return Optional.ofNullable(usersById.get(playerId));
      }

      @Override
      public Optional<User> findByUsername(final Username username) {

        return usersById.values().stream()
            .filter(user -> user.username().value().equals(username.value())).findFirst();
      }

      @Override
      public boolean existsByUsername(final Username username) {

        return findByUsername(username).isPresent();
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
        userRepositoryWith(Map.of(playerId, rehydratedUser(playerId, "juancho"))),
        emptyBotRegistry());

    assertThat(resolver.resolve(playerId)).isEqualTo("juancho");
  }

  @Test
  @DisplayName("usa displayName para bots")
  void resolvesBotToDisplayName() {

    final var botId = PlayerId.generate();
    final var bot = new BotProfile(botId, "El Mentiroso", new BotPersonality(90, 20, 70, 50, 30));
    final var resolver = new DefaultPublicActorResolver(userRepositoryWith(Map.of()),
        botRegistryWith(bot));

    assertThat(resolver.resolve(botId)).isEqualTo("El Mentiroso");
  }

  @Test
  @DisplayName("usa alias de invitado para players sin cuenta ni bot")
  void resolvesGuestToSyntheticDisplayName() {

    final var guestId = PlayerId.of("12345678-1234-1234-1234-1234567890ab");
    final var resolver = new DefaultPublicActorResolver(userRepositoryWith(Map.of()),
        emptyBotRegistry());

    assertThat(resolver.resolve(guestId)).isEqualTo("Invitado-12345678");
  }

  @Test
  @DisplayName("prioriza username cuando existe usuario y no cae en fallback")
  void doesNotFallbackWhenUserExists() {

    final var playerId = PlayerId.of("aaaaaaaa-1234-1234-1234-1234567890ab");
    final var resolver = new DefaultPublicActorResolver(
        userRepositoryWith(Map.of(playerId, rehydratedUser(playerId, "martina"))),
        emptyBotRegistry());

    assertThat(resolver.resolve(playerId)).isEqualTo("martina");
  }

}
