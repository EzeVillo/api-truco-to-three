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
import com.villo.truco.social.application.commands.RequestFriendshipCommand;
import com.villo.truco.social.application.exceptions.FriendshipAlreadyExistsException;
import com.villo.truco.social.application.exceptions.FriendshipRequestAlreadyPendingException;
import com.villo.truco.social.application.exceptions.SocialFeatureRequiresRegisteredUserException;
import com.villo.truco.social.application.services.SocialUserGuard;
import com.villo.truco.social.domain.model.friendship.Friendship;
import com.villo.truco.social.domain.model.friendship.exceptions.FriendRequestsNotAcceptedException;
import com.villo.truco.social.domain.model.preferences.SocialPreferences;
import com.villo.truco.social.domain.ports.FriendshipQueryRepository;
import com.villo.truco.social.domain.ports.FriendshipRepository;
import com.villo.truco.social.domain.ports.SocialPreferencesRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("RequestFriendshipCommandHandler")
class RequestFriendshipCommandHandlerTest {

  @Test
  @DisplayName("usa lookup exacto por username y crea la solicitud")
  void createsFriendshipByExactUsernameLookup() {

    final var requester = PlayerId.generate();
    final var addressee = PlayerId.generate();
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(Map.of(requester, "juancho"));
    when(userRepo.findUserIdByUsername("martina")).thenReturn(Optional.of(addressee));

    final var friendshipQueryRepo = mock(FriendshipQueryRepository.class);
    final var friendshipRepo = mock(FriendshipRepository.class);
    final var preferencesRepo = mock(SocialPreferencesRepository.class);
    final var handler = new RequestFriendshipCommandHandler(new SocialUserGuard(userRepo),
        friendshipQueryRepo, friendshipRepo, preferencesRepo, events -> {
    });

    handler.handle(new RequestFriendshipCommand(requester.value().toString(), "martina"));

    final var captor = ArgumentCaptor.forClass(Friendship.class);
    verify(friendshipRepo).save(captor.capture());
    assertThat(captor.getValue().getRequesterId()).isEqualTo(requester);
    assertThat(captor.getValue().getAddresseeId()).isEqualTo(addressee);
  }

  @Test
  @DisplayName("rechaza si el destinatario no acepta solicitudes de amistad")
  void rejectsWhenAddresseeDoesNotAcceptRequests() {

    final var requester = PlayerId.generate();
    final var addressee = PlayerId.generate();
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(Map.of(requester, "juancho"));
    when(userRepo.findUserIdByUsername("martina")).thenReturn(Optional.of(addressee));

    final var friendshipQueryRepo = mock(FriendshipQueryRepository.class);
    final var friendshipRepo = mock(FriendshipRepository.class);
    final var preferencesRepo = mock(SocialPreferencesRepository.class);
    when(preferencesRepo.findByPlayerId(addressee)).thenReturn(
        Optional.of(SocialPreferences.reconstruct(addressee, false)));

    final var handler = new RequestFriendshipCommandHandler(new SocialUserGuard(userRepo),
        friendshipQueryRepo, friendshipRepo, preferencesRepo, events -> {
    });

    assertThatThrownBy(() -> handler.handle(
        new RequestFriendshipCommand(requester.value().toString(), "martina"))).isInstanceOf(
        FriendRequestsNotAcceptedException.class);
    verify(friendshipRepo, never()).save(any());
  }

  @Test
  @DisplayName("rechaza si ya hay una solicitud pendiente entre los jugadores")
  void rejectsDuplicatedPendingFriendshipRequest() {

    final var requester = PlayerId.generate();
    final var addressee = PlayerId.generate();
    final var existing = Friendship.request(requester, addressee, true);
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(Map.of(requester, "juancho"));
    when(userRepo.findUserIdByUsername("martina")).thenReturn(Optional.of(addressee));

    final var friendshipQueryRepo = mock(FriendshipQueryRepository.class);
    when(friendshipQueryRepo.findPendingByPlayers(any(), any())).thenReturn(Optional.of(existing));

    final var handler = new RequestFriendshipCommandHandler(new SocialUserGuard(userRepo),
        friendshipQueryRepo, friendship -> {
    }, mock(SocialPreferencesRepository.class), events -> {
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
    final var existing = Friendship.request(requester, addressee, true);
    existing.accept(addressee);
    final var userRepo = mock(UserQueryRepository.class);
    when(userRepo.findUsernamesByIds(anySet())).thenReturn(Map.of(requester, "juancho"));
    when(userRepo.findUserIdByUsername("martina")).thenReturn(Optional.of(addressee));

    final var friendshipQueryRepo = mock(FriendshipQueryRepository.class);
    when(friendshipQueryRepo.findAcceptedByPlayers(any(), any())).thenReturn(Optional.of(existing));

    final var handler = new RequestFriendshipCommandHandler(new SocialUserGuard(userRepo),
        friendshipQueryRepo, friendship -> {
    }, mock(SocialPreferencesRepository.class), events -> {
    });

    assertThatThrownBy(() -> handler.handle(
        new RequestFriendshipCommand(requester.value().toString(), "martina"))).isInstanceOf(
        FriendshipAlreadyExistsException.class);
  }

  @Test
  @DisplayName("rechaza usuarios guest para funcionalidades sociales")
  void rejectsGuests() {

    final var guest = PlayerId.generate();
    final var userRepo = mock(UserQueryRepository.class);
    final var handler = new RequestFriendshipCommandHandler(new SocialUserGuard(userRepo),
        mock(FriendshipQueryRepository.class), friendship -> {
    }, mock(SocialPreferencesRepository.class), events -> {
    });

    assertThatThrownBy(() -> handler.handle(
        new RequestFriendshipCommand(guest.value().toString(), "martina"))).isInstanceOf(
        SocialFeatureRequiresRegisteredUserException.class);
  }

}
