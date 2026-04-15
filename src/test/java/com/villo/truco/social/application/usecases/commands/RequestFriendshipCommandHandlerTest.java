package com.villo.truco.social.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.commands.RequestFriendshipCommand;
import com.villo.truco.social.application.exceptions.FriendshipAlreadyExistsException;
import com.villo.truco.social.application.exceptions.FriendshipRequestAlreadyPendingException;
import com.villo.truco.social.application.exceptions.SocialFeatureRequiresRegisteredUserException;
import com.villo.truco.social.application.services.SocialUserGuard;
import com.villo.truco.social.domain.model.friendship.Friendship;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipId;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import com.villo.truco.social.domain.ports.FriendshipRepository;
import com.villo.truco.social.domain.ports.SocialEventNotifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RequestFriendshipCommandHandler")
class RequestFriendshipCommandHandlerTest {

  @Test
  @DisplayName("usa lookup exacto por username y crea la solicitud")
  void createsFriendshipByExactUsernameLookup() {

    final var requester = PlayerId.generate();
    final var addressee = PlayerId.generate();
    final var saved = new AtomicReference<Friendship>();
    final var userQueryRepository = new TestUserQueryRepository(
        Map.of(requester, "juancho", addressee, "martina"));
    final FriendshipQueryRepository friendshipQueryRepository = new EmptyFriendshipQueryRepository();
    final FriendshipRepository friendshipRepository = saved::set;
    final SocialEventNotifier socialEventNotifier = events -> {
    };
    final var handler = new RequestFriendshipCommandHandler(
        new SocialUserGuard(userQueryRepository), friendshipQueryRepository, friendshipRepository,
        socialEventNotifier);

    handler.handle(new RequestFriendshipCommand(requester.value().toString(), "martina"));

    assertThat(saved.get()).isNotNull();
    assertThat(saved.get().getRequesterId()).isEqualTo(requester);
    assertThat(saved.get().getAddresseeId()).isEqualTo(addressee);
  }

  @Test
  @DisplayName("rechaza si ya hay una solicitud pendiente entre los jugadores")
  void rejectsDuplicatedPendingFriendshipRequest() {

    final var requester = PlayerId.generate();
    final var addressee = PlayerId.generate();
    final var existing = Friendship.request(requester, addressee);
    final var userQueryRepository = new TestUserQueryRepository(
        Map.of(requester, "juancho", addressee, "martina"));
    final FriendshipQueryRepository friendshipQueryRepository = new FriendshipQueryRepository() {
      @Override
      public Optional<Friendship> findById(final FriendshipId friendshipId) {

        return Optional.empty();
      }

      @Override
      public boolean existsAcceptedByPlayers(final PlayerId firstPlayerId,
          final PlayerId secondPlayerId) {

        return false;
      }

      @Override
      public Optional<Friendship> findPendingByPlayers(final PlayerId firstPlayerId,
          final PlayerId secondPlayerId) {

        return Optional.of(existing);
      }

      @Override
      public Optional<Friendship> findPendingByRequesterAndAddressee(final PlayerId requesterId,
          final PlayerId addresseeId) {

        return Optional.empty();
      }

      @Override
      public Optional<Friendship> findAcceptedByPlayers(final PlayerId firstPlayerId,
          final PlayerId secondPlayerId) {

        return Optional.empty();
      }

      @Override
      public List<Friendship> findAcceptedByPlayer(final PlayerId playerId) {

        return List.of();
      }

      @Override
      public List<Friendship> findPendingReceivedBy(final PlayerId playerId) {

        return List.of();
      }

      @Override
      public List<Friendship> findPendingSentBy(final PlayerId playerId) {

        return List.of();
      }
    };
    final var handler = new RequestFriendshipCommandHandler(
        new SocialUserGuard(userQueryRepository), friendshipQueryRepository, friendship -> {
    }, events -> {
    });

    assertThatThrownBy(() -> handler.handle(
        new RequestFriendshipCommand(requester.value().toString(), "martina"))).isInstanceOf(
        FriendshipRequestAlreadyPendingException.class);
  }

  @Test
  @DisplayName("rechaza si los jugadores ya son amigos")
  void rejectsWhenFriendshipAlreadyAccepted() {

    final var requester = PlayerId.generate();
    final var addressee = PlayerId.generate();
    final var existing = Friendship.request(requester, addressee);
    existing.accept(addressee);
    final var userQueryRepository = new TestUserQueryRepository(
        Map.of(requester, "juancho", addressee, "martina"));
    final FriendshipQueryRepository friendshipQueryRepository = new FriendshipQueryRepository() {
      @Override
      public Optional<Friendship> findById(final FriendshipId friendshipId) {

        return Optional.empty();
      }

      @Override
      public boolean existsAcceptedByPlayers(final PlayerId firstPlayerId,
          final PlayerId secondPlayerId) {

        return false;
      }

      @Override
      public Optional<Friendship> findPendingByPlayers(final PlayerId firstPlayerId,
          final PlayerId secondPlayerId) {

        return Optional.empty();
      }

      @Override
      public Optional<Friendship> findPendingByRequesterAndAddressee(final PlayerId requesterId,
          final PlayerId addresseeId) {

        return Optional.empty();
      }

      @Override
      public Optional<Friendship> findAcceptedByPlayers(final PlayerId firstPlayerId,
          final PlayerId secondPlayerId) {

        return Optional.of(existing);
      }

      @Override
      public List<Friendship> findAcceptedByPlayer(final PlayerId playerId) {

        return List.of();
      }

      @Override
      public List<Friendship> findPendingReceivedBy(final PlayerId playerId) {

        return List.of();
      }

      @Override
      public List<Friendship> findPendingSentBy(final PlayerId playerId) {

        return List.of();
      }
    };
    final var handler = new RequestFriendshipCommandHandler(
        new SocialUserGuard(userQueryRepository), friendshipQueryRepository, friendship -> {
    }, events -> {
    });

    assertThatThrownBy(() -> handler.handle(
        new RequestFriendshipCommand(requester.value().toString(), "martina"))).isInstanceOf(
        FriendshipAlreadyExistsException.class);
  }

  @Test
  @DisplayName("rechaza usuarios guest para funcionalidades sociales")
  void rejectsGuests() {

    final var guest = PlayerId.generate();
    final var userQueryRepository = new TestUserQueryRepository(Map.of());
    final var handler = new RequestFriendshipCommandHandler(
        new SocialUserGuard(userQueryRepository), new EmptyFriendshipQueryRepository(),
        friendship -> {
        }, events -> {
    });

    assertThatThrownBy(() -> handler.handle(
        new RequestFriendshipCommand(guest.value().toString(), "martina"))).isInstanceOf(
        SocialFeatureRequiresRegisteredUserException.class);
  }

  private record TestUserQueryRepository(Map<PlayerId, String> usernamesById) implements
      UserQueryRepository {

    private TestUserQueryRepository(final Map<PlayerId, String> usernamesById) {

      this.usernamesById = new LinkedHashMap<>(usernamesById);
    }

    @Override
    public Map<PlayerId, String> findUsernamesByIds(final Set<PlayerId> playerIds) {

      final var result = new LinkedHashMap<PlayerId, String>();
      for (final var playerId : playerIds) {
        final var username = this.usernamesById.get(playerId);
        if (username != null) {
          result.put(playerId, username);
        }
      }
      return result;
    }

    @Override
    public Optional<PlayerId> findUserIdByUsername(final String username) {

      return this.usernamesById.entrySet().stream()
          .filter(entry -> entry.getValue().equals(username)).map(Entry::getKey).findFirst();
    }

  }

  private static final class EmptyFriendshipQueryRepository implements FriendshipQueryRepository {

    @Override
    public Optional<Friendship> findById(final FriendshipId friendshipId) {

      return Optional.empty();
    }

    @Override
    public boolean existsAcceptedByPlayers(final PlayerId firstPlayerId,
        final PlayerId secondPlayerId) {

      return false;
    }

    @Override
    public Optional<Friendship> findPendingByPlayers(final PlayerId firstPlayerId,
        final PlayerId secondPlayerId) {

      return Optional.empty();
    }

    @Override
    public Optional<Friendship> findPendingByRequesterAndAddressee(final PlayerId requesterId,
        final PlayerId addresseeId) {

      return Optional.empty();
    }

    @Override
    public Optional<Friendship> findAcceptedByPlayers(final PlayerId firstPlayerId,
        final PlayerId secondPlayerId) {

      return Optional.empty();
    }

    @Override
    public List<Friendship> findAcceptedByPlayer(final PlayerId playerId) {

      return List.of();
    }

    @Override
    public List<Friendship> findPendingReceivedBy(final PlayerId playerId) {

      return List.of();
    }

    @Override
    public List<Friendship> findPendingSentBy(final PlayerId playerId) {

      return List.of();
    }

  }

}
