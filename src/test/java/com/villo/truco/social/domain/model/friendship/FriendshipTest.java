package com.villo.truco.social.domain.model.friendship;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.friendship.events.FriendRequestAcceptedEvent;
import com.villo.truco.social.domain.model.friendship.events.FriendRequestReceivedEvent;
import com.villo.truco.social.domain.model.friendship.exceptions.CannotFriendYourselfException;
import com.villo.truco.social.domain.model.friendship.exceptions.OnlyAddresseeCanRespondFriendRequestException;
import com.villo.truco.social.domain.model.friendship.valueobjects.FriendshipStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Friendship")
class FriendshipTest {

  @Test
  @DisplayName("crea solicitud pendiente y emite evento de recepcion")
  void requestCreatesPendingFriendship() {

    final var requester = PlayerId.generate();
    final var addressee = PlayerId.generate();

    final var friendship = Friendship.request(requester, addressee);

    assertThat(friendship.getRequesterId()).isEqualTo(requester);
    assertThat(friendship.getAddresseeId()).isEqualTo(addressee);
    assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.PENDING);
    assertThat(friendship.getFriendshipDomainEvents()).singleElement()
        .isInstanceOf(FriendRequestReceivedEvent.class);
  }

  @Test
  @DisplayName("acepta la solicitud solo el destinatario y emite evento")
  void addresseeAcceptsPendingFriendship() {

    final var requester = PlayerId.generate();
    final var addressee = PlayerId.generate();
    final var friendship = Friendship.request(requester, addressee);
    friendship.clearDomainEvents();

    friendship.accept(addressee);

    assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
    assertThat(friendship.getFriendshipDomainEvents()).singleElement()
        .isInstanceOf(FriendRequestAcceptedEvent.class);
  }

  @Test
  @DisplayName("rechaza auto amistad")
  void rejectsSelfFriendship() {

    final var player = PlayerId.generate();

    assertThatThrownBy(() -> Friendship.request(player, player)).isInstanceOf(
        CannotFriendYourselfException.class);
  }

  @Test
  @DisplayName("impide aceptar a quien no es destinatario")
  void rejectsAcceptFromNonAddressee() {

    final var requester = PlayerId.generate();
    final var addressee = PlayerId.generate();
    final var friendship = Friendship.request(requester, addressee);

    assertThatThrownBy(() -> friendship.accept(requester)).isInstanceOf(
        OnlyAddresseeCanRespondFriendRequestException.class);
  }

}
