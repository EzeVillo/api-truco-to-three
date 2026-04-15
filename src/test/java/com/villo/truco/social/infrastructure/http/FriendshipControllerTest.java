package com.villo.truco.social.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.social.application.dto.FriendSummaryDTO;
import com.villo.truco.social.application.dto.IncomingFriendshipRequestDTO;
import com.villo.truco.social.application.dto.OutgoingFriendshipRequestDTO;
import com.villo.truco.social.application.ports.in.AcceptFriendshipUseCase;
import com.villo.truco.social.application.ports.in.CancelFriendshipUseCase;
import com.villo.truco.social.application.ports.in.DeclineFriendshipUseCase;
import com.villo.truco.social.application.ports.in.GetFriendsUseCase;
import com.villo.truco.social.application.ports.in.GetFriendshipRequestsUseCase;
import com.villo.truco.social.application.ports.in.GetSentFriendshipRequestsUseCase;
import com.villo.truco.social.application.ports.in.RemoveFriendshipUseCase;
import com.villo.truco.social.application.ports.in.RequestFriendshipUseCase;
import com.villo.truco.social.infrastructure.http.dto.request.RequestFriendshipRequest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayName("FriendshipController")
class FriendshipControllerTest {

  @Test
  @DisplayName("expone endpoints de amistad con codigos y payload esperados")
  void flows() {

    final var actorId = "11111111-1111-1111-1111-111111111111";
    final RequestFriendshipUseCase requestFriendshipUseCase = mock(RequestFriendshipUseCase.class);
    final AcceptFriendshipUseCase acceptFriendshipUseCase = mock(AcceptFriendshipUseCase.class);
    final DeclineFriendshipUseCase declineFriendshipUseCase = mock(DeclineFriendshipUseCase.class);
    final CancelFriendshipUseCase cancelFriendshipUseCase = mock(CancelFriendshipUseCase.class);
    final RemoveFriendshipUseCase removeFriendshipUseCase = mock(RemoveFriendshipUseCase.class);
    final GetFriendsUseCase getFriendsUseCase = mock(GetFriendsUseCase.class);
    final GetFriendshipRequestsUseCase getFriendshipRequestsUseCase = mock(
        GetFriendshipRequestsUseCase.class);
    final GetSentFriendshipRequestsUseCase getSentFriendshipRequestsUseCase = mock(
        GetSentFriendshipRequestsUseCase.class);
    when(getFriendsUseCase.handle(any())).thenReturn(List.of(new FriendSummaryDTO("martina")));
    when(getFriendshipRequestsUseCase.handle(any())).thenReturn(
        List.of(new IncomingFriendshipRequestDTO("agus")));
    when(getSentFriendshipRequestsUseCase.handle(any())).thenReturn(
        List.of(new OutgoingFriendshipRequestDTO("martina")));

    final var controller = new FriendshipController(requestFriendshipUseCase,
        acceptFriendshipUseCase, declineFriendshipUseCase, cancelFriendshipUseCase,
        removeFriendshipUseCase, getFriendsUseCase, getFriendshipRequestsUseCase,
        getSentFriendshipRequestsUseCase);
    final var jwt = Jwt.withTokenValue("token").header("alg", "none").subject(actorId).build();

    assertThat(controller.requestFriendship(new RequestFriendshipRequest("martina"), jwt)
        .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(controller.acceptFriendship("martina", jwt).getStatusCode()).isEqualTo(
        HttpStatus.NO_CONTENT);
    assertThat(controller.declineFriendship("martina", jwt).getStatusCode()).isEqualTo(
        HttpStatus.NO_CONTENT);
    assertThat(controller.cancelFriendship("martina", jwt).getStatusCode()).isEqualTo(
        HttpStatus.NO_CONTENT);
    assertThat(controller.removeFriendship("martina", jwt).getStatusCode()).isEqualTo(
        HttpStatus.NO_CONTENT);
    assertThat(controller.getFriends(jwt).getBody()).hasSize(1);
    assertThat(controller.getFriends(jwt).getBody().getFirst().friendUsername()).isEqualTo(
        "martina");
    assertThat(controller.getFriendshipRequests(jwt).getBody()).hasSize(1);
    assertThat(
        controller.getFriendshipRequests(jwt).getBody().getFirst().requesterUsername()).isEqualTo(
        "agus");
    assertThat(controller.getSentFriendshipRequests(jwt).getBody()).hasSize(1);
    assertThat(controller.getSentFriendshipRequests(jwt).getBody().getFirst()
        .addresseeUsername()).isEqualTo("martina");
  }

}
