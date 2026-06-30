package com.villo.truco.social.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.commands.AcceptFriendshipCommand;
import com.villo.truco.social.application.commands.CancelFriendshipCommand;
import com.villo.truco.social.application.commands.RemoveFriendshipCommand;
import com.villo.truco.social.application.exceptions.FriendshipNotFoundException;
import com.villo.truco.social.application.exceptions.SocialUserNotFoundException;
import com.villo.truco.social.application.services.SocialUserGuard;
import com.villo.truco.social.domain.model.friendship.Friendship;
import com.villo.truco.social.domain.model.friendship.FriendshipLimitPolicy;
import com.villo.truco.social.domain.model.friendship.exceptions.FriendLimitReachedException;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import com.villo.truco.social.domain.ports.FriendshipRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("Friendship command handlers by username")
class FriendshipCommandHandlersByUsernameTest {

  @Test
  @DisplayName("accept resolves pending request by requester username")
  void acceptResolvesPendingRequestByRequesterUsername() {

    final var requester = PlayerId.generate();
    final var addressee = PlayerId.generate();
    final var friendship = Friendship.request(requester, addressee, true);
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(Map.of(addressee, "martina"));
    when(userRepo.findUserIdByUsername("juancho")).thenReturn(Optional.of(requester));

    final var friendshipQueryRepo = mock(FriendshipQueryRepository.class);
    when(friendshipQueryRepo.findPendingByRequesterAndAddressee(requester, addressee)).thenReturn(
        Optional.of(friendship));

    final var friendshipRepo = mock(FriendshipRepository.class);
    final var handler = new AcceptFriendshipCommandHandler(new SocialUserGuard(userRepo),
        friendshipQueryRepo, friendshipRepo, new FriendshipLimitPolicy(friendshipQueryRepo, 10),
        events -> {
        });

    handler.handle(new AcceptFriendshipCommand("juancho", addressee.value().toString()));

    final var captor = ArgumentCaptor.forClass(Friendship.class);
    verify(friendshipRepo).save(captor.capture());
    assertThat(captor.getValue()).isSameAs(friendship);
    assertThat(friendship.isAccepted()).isTrue();
  }

  @Test
  @DisplayName("accept rechaza cuando el que acepta ya alcanzó el máximo de amigos")
  void acceptRejectsWhenAccepterReachedFriendLimit() {

    final var requester = PlayerId.generate();
    final var addressee = PlayerId.generate();
    final var friendship = Friendship.request(requester, addressee, true);
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(Map.of(addressee, "martina"));
    when(userRepo.findUserIdByUsername("juancho")).thenReturn(Optional.of(requester));

    final var friendshipQueryRepo = mock(FriendshipQueryRepository.class);
    when(friendshipQueryRepo.findPendingByRequesterAndAddressee(requester, addressee)).thenReturn(
        Optional.of(friendship));
    when(friendshipQueryRepo.countAcceptedByPlayer(addressee)).thenReturn(10);

    final var friendshipRepo = mock(FriendshipRepository.class);
    final var handler = new AcceptFriendshipCommandHandler(new SocialUserGuard(userRepo),
        friendshipQueryRepo, friendshipRepo, new FriendshipLimitPolicy(friendshipQueryRepo, 10),
        events -> {
        });

    assertThatThrownBy(() -> handler.handle(
        new AcceptFriendshipCommand("juancho", addressee.value().toString()))).isInstanceOf(
        FriendLimitReachedException.class);
    verify(friendshipRepo, never()).save(any());
    assertThat(friendship.isAccepted()).isFalse();
  }

  @Test
  @DisplayName("accept fails when actor is not the addressee of the pending request")
  void acceptFailsWhenActorIsNotAddressee() {

    final var requester = PlayerId.generate();
    final var addressee = PlayerId.generate();
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(Map.of(requester, "juancho"));
    when(userRepo.findUserIdByUsername("martina")).thenReturn(Optional.of(addressee));

    final var handler = new AcceptFriendshipCommandHandler(new SocialUserGuard(userRepo),
        mock(FriendshipQueryRepository.class), friendship -> {
    }, new FriendshipLimitPolicy(mock(FriendshipQueryRepository.class), 10), events -> {
    });

    assertThatThrownBy(() -> handler.handle(
        new AcceptFriendshipCommand("martina", requester.value().toString()))).isInstanceOf(
        FriendshipNotFoundException.class);
  }

  @Test
  @DisplayName("cancel fails when actor is not the requester of the pending request")
  void cancelFailsWhenActorIsNotRequester() {

    final var requester = PlayerId.generate();
    final var addressee = PlayerId.generate();
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(Map.of(addressee, "martina"));
    when(userRepo.findUserIdByUsername("juancho")).thenReturn(Optional.of(requester));

    final var handler = new CancelFriendshipCommandHandler(new SocialUserGuard(userRepo),
        mock(FriendshipQueryRepository.class), friendship -> {
    }, events -> {
    });

    assertThatThrownBy(() -> handler.handle(
        new CancelFriendshipCommand("juancho", addressee.value().toString()))).isInstanceOf(
        FriendshipNotFoundException.class);
  }

  @Test
  @DisplayName("remove fails when the friendship between both usernames is not accepted")
  void removeFailsWhenFriendshipIsNotAccepted() {

    final var requester = PlayerId.generate();
    final var addressee = PlayerId.generate();
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(Map.of(requester, "juancho"));
    when(userRepo.findUserIdByUsername("martina")).thenReturn(Optional.of(addressee));

    final var handler = new RemoveFriendshipCommandHandler(new SocialUserGuard(userRepo),
        mock(FriendshipQueryRepository.class), friendship -> {
    }, events -> {
    });

    assertThatThrownBy(() -> handler.handle(
        new RemoveFriendshipCommand("martina", requester.value().toString()))).isInstanceOf(
        FriendshipNotFoundException.class);
  }

  @Test
  @DisplayName("mutations fail when username does not belong to a registered user")
  void failsWhenUsernameDoesNotExist() {

    final var actor = PlayerId.generate();
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(Map.of(actor, "juancho"));

    final var handler = new AcceptFriendshipCommandHandler(new SocialUserGuard(userRepo),
        mock(FriendshipQueryRepository.class), friendship -> {
    }, new FriendshipLimitPolicy(mock(FriendshipQueryRepository.class), 10), events -> {
    });

    assertThatThrownBy(() -> handler.handle(
        new AcceptFriendshipCommand("martina", actor.value().toString()))).isInstanceOf(
        SocialUserNotFoundException.class);
  }

}
