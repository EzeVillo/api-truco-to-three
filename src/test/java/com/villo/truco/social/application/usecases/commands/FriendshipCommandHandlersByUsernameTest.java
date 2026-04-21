package com.villo.truco.social.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.commands.AcceptFriendshipCommand;
import com.villo.truco.social.application.commands.CancelFriendshipCommand;
import com.villo.truco.social.application.commands.RemoveFriendshipCommand;
import com.villo.truco.social.application.exceptions.FriendshipNotFoundException;
import com.villo.truco.social.application.exceptions.SocialUserNotFoundException;
import com.villo.truco.social.application.services.SocialUserGuard;
import com.villo.truco.social.domain.model.friendship.Friendship;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipId;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
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

@DisplayName("Friendship command handlers by username")
class FriendshipCommandHandlersByUsernameTest {

  private static SocialEventNotifier noopNotifier() {

    return events -> {
    };
  }

  @Test
  @DisplayName("accept resolves pending request by requester username")
  void acceptResolvesPendingRequestByRequesterUsername() {

    final var requester = PlayerId.generate();
    final var addressee = PlayerId.generate();
    final var friendship = Friendship.request(requester, addressee);
    final var saved = new AtomicReference<Friendship>();
    final var userQueryRepository = new TestUserQueryRepository(
        Map.of(requester, "juancho", addressee, "martina"));
    final var handler = new AcceptFriendshipCommandHandler(new SocialUserGuard(userQueryRepository),
        new InMemoryFriendshipQueryRepository(List.of(friendship)), saved::set, noopNotifier());

    handler.handle(new AcceptFriendshipCommand("juancho", addressee.value().toString()));

    assertThat(saved.get()).isSameAs(friendship);
    assertThat(friendship.isAccepted()).isTrue();
  }

  @Test
  @DisplayName("accept fails when actor is not the addressee of the pending request")
  void acceptFailsWhenActorIsNotAddressee() {

    final var requester = PlayerId.generate();
    final var addressee = PlayerId.generate();
    final var friendship = Friendship.request(requester, addressee);
    final var userQueryRepository = new TestUserQueryRepository(
        Map.of(requester, "juancho", addressee, "martina"));
    final var handler = new AcceptFriendshipCommandHandler(new SocialUserGuard(userQueryRepository),
        new InMemoryFriendshipQueryRepository(List.of(friendship)), ignored -> {
    }, noopNotifier());

    assertThatThrownBy(() -> handler.handle(
        new AcceptFriendshipCommand("martina", requester.value().toString()))).isInstanceOf(
        FriendshipNotFoundException.class);
  }

  @Test
  @DisplayName("cancel fails when actor is not the requester of the pending request")
  void cancelFailsWhenActorIsNotRequester() {

    final var requester = PlayerId.generate();
    final var addressee = PlayerId.generate();
    final var friendship = Friendship.request(requester, addressee);
    final var userQueryRepository = new TestUserQueryRepository(
        Map.of(requester, "juancho", addressee, "martina"));
    final var handler = new CancelFriendshipCommandHandler(new SocialUserGuard(userQueryRepository),
        new InMemoryFriendshipQueryRepository(List.of(friendship)), ignored -> {
    }, noopNotifier());

    assertThatThrownBy(() -> handler.handle(
        new CancelFriendshipCommand("juancho", addressee.value().toString()))).isInstanceOf(
        FriendshipNotFoundException.class);
  }

  @Test
  @DisplayName("remove fails when the friendship between both usernames is not accepted")
  void removeFailsWhenFriendshipIsNotAccepted() {

    final var requester = PlayerId.generate();
    final var addressee = PlayerId.generate();
    final var friendship = Friendship.request(requester, addressee);
    final var userQueryRepository = new TestUserQueryRepository(
        Map.of(requester, "juancho", addressee, "martina"));
    final var handler = new RemoveFriendshipCommandHandler(new SocialUserGuard(userQueryRepository),
        new InMemoryFriendshipQueryRepository(List.of(friendship)), ignored -> {
    }, noopNotifier());

    assertThatThrownBy(() -> handler.handle(
        new RemoveFriendshipCommand("martina", requester.value().toString()))).isInstanceOf(
        FriendshipNotFoundException.class);
  }

  @Test
  @DisplayName("mutations fail when username does not belong to a registered user")
  void failsWhenUsernameDoesNotExist() {

    final var actor = PlayerId.generate();
    final var userQueryRepository = new TestUserQueryRepository(Map.of(actor, "juancho"));
    final var handler = new AcceptFriendshipCommandHandler(new SocialUserGuard(userQueryRepository),
        new InMemoryFriendshipQueryRepository(List.of()), ignored -> {
    }, noopNotifier());

    assertThatThrownBy(() -> handler.handle(
        new AcceptFriendshipCommand("martina", actor.value().toString()))).isInstanceOf(
        SocialUserNotFoundException.class);
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

  private record InMemoryFriendshipQueryRepository(List<Friendship> friendships) implements
      FriendshipQueryRepository {

    private static boolean samePlayers(final Friendship friendship, final PlayerId firstPlayerId,
        final PlayerId secondPlayerId) {

      return friendship.getRequesterId().equals(firstPlayerId) && friendship.getAddresseeId()
          .equals(secondPlayerId)
          || friendship.getRequesterId().equals(secondPlayerId) && friendship.getAddresseeId()
          .equals(firstPlayerId);
    }

    @Override
    public Optional<Friendship> findById(final FriendshipId friendshipId) {

      return this.friendships.stream().filter(friendship -> friendship.getId().equals(friendshipId))
          .findFirst();
    }

    @Override
    public boolean existsAcceptedByPlayers(final PlayerId firstPlayerId,
        final PlayerId secondPlayerId) {

      return this.findAcceptedByPlayers(firstPlayerId, secondPlayerId).isPresent();
    }

    @Override
    public Optional<Friendship> findPendingByPlayers(final PlayerId firstPlayerId,
        final PlayerId secondPlayerId) {

      return this.friendships.stream().filter(friendship -> !friendship.isAccepted())
          .filter(friendship -> friendship.getStatus().name().equals("PENDING"))
          .filter(friendship -> samePlayers(friendship, firstPlayerId, secondPlayerId)).findFirst();
    }

    @Override
    public Optional<Friendship> findPendingByRequesterAndAddressee(final PlayerId requesterId,
        final PlayerId addresseeId) {

      return this.friendships.stream()
          .filter(friendship -> friendship.getRequesterId().equals(requesterId))
          .filter(friendship -> friendship.getAddresseeId().equals(addresseeId))
          .filter(friendship -> friendship.getStatus().name().equals("PENDING")).findFirst();
    }

    @Override
    public Optional<Friendship> findAcceptedByPlayers(final PlayerId firstPlayerId,
        final PlayerId secondPlayerId) {

      return this.friendships.stream().filter(Friendship::isAccepted)
          .filter(friendship -> samePlayers(friendship, firstPlayerId, secondPlayerId)).findFirst();
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
