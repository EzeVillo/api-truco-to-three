package com.villo.truco.social.application.usecases.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.dto.FriendActivityDTO;
import com.villo.truco.social.application.dto.FriendActivityStateDTO;
import com.villo.truco.social.application.queries.GetFriendActivityQuery;
import com.villo.truco.social.application.services.FriendActivityResolver;
import com.villo.truco.social.application.services.SocialUserGuard;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GetFriendActivityQueryHandler")
class GetFriendActivityQueryHandlerTest {

  @Test
  @DisplayName("devuelve snapshot de actividad para usuario registrado")
  void returnsFriendActivityStateForRegisteredUser() {

    final var player = PlayerId.generate();
    final var userRepository = mock(UserQueryRepository.class);
    when(userRepository.findUsernamesByIds(anySet())).thenReturn(Map.of(player, "ana"));
    final var resolver = mock(FriendActivityResolver.class);
    when(resolver.resolveState(player)).thenReturn(
        new FriendActivityStateDTO(List.of(new FriendActivityDTO("martina", null))));
    final var handler = new GetFriendActivityQueryHandler(new SocialUserGuard(userRepository),
        resolver);

    final var state = handler.handle(new GetFriendActivityQuery(player));

    assertThat(state.friends()).singleElement().extracting(FriendActivityDTO::friendUsername)
        .isEqualTo("martina");
  }

}
