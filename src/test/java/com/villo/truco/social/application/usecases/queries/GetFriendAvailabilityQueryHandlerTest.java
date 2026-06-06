package com.villo.truco.social.application.usecases.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.dto.FriendAvailabilityDTO;
import com.villo.truco.social.application.dto.FriendAvailabilityStateDTO;
import com.villo.truco.social.application.dto.FriendAvailabilityStatus;
import com.villo.truco.social.application.exceptions.SocialFeatureRequiresRegisteredUserException;
import com.villo.truco.social.application.queries.GetFriendAvailabilityQuery;
import com.villo.truco.social.application.services.FriendAvailabilityResolver;
import com.villo.truco.social.application.services.SocialUserGuard;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GetFriendAvailabilityQueryHandler")
class GetFriendAvailabilityQueryHandlerTest {

  @Test
  @DisplayName("resuelve el snapshot de disponibilidad del jugador autenticado")
  void resolvesAvailabilitySnapshot() {

    final var guard = mock(SocialUserGuard.class);
    final var resolver = mock(FriendAvailabilityResolver.class);
    final var handler = new GetFriendAvailabilityQueryHandler(guard, resolver);
    final var player = PlayerId.generate();
    final var snapshot = new FriendAvailabilityStateDTO(List.of(
        new FriendAvailabilityDTO("martina", true, FriendAvailabilityStatus.AVAILABLE, null,
            null)));
    when(resolver.resolveState(player)).thenReturn(snapshot);

    final var result = handler.handle(new GetFriendAvailabilityQuery(player));

    assertThat(result).isSameAs(snapshot);
    verify(guard).ensureRegisteredUser(player);
  }

  @Test
  @DisplayName("rechaza usuarios no registrados sin resolver disponibilidad")
  void rejectsUnregisteredUsers() {

    final var guard = mock(SocialUserGuard.class);
    final var resolver = mock(FriendAvailabilityResolver.class);
    final var handler = new GetFriendAvailabilityQueryHandler(guard, resolver);
    final var player = PlayerId.generate();
    doThrow(new SocialFeatureRequiresRegisteredUserException()).when(guard)
        .ensureRegisteredUser(player);

    assertThatThrownBy(() -> handler.handle(new GetFriendAvailabilityQuery(player))).isInstanceOf(
        SocialFeatureRequiresRegisteredUserException.class);
  }

}
