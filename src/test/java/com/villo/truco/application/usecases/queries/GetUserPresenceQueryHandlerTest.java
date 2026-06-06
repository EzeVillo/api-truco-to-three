package com.villo.truco.application.usecases.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.application.dto.ActiveMatchRefDTO;
import com.villo.truco.application.dto.UserPresenceDTO;
import com.villo.truco.application.queries.GetUserPresenceQuery;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GetUserPresenceQueryHandler")
class GetUserPresenceQueryHandlerTest {

  @Test
  @DisplayName("delega la resolucion en el UserPresenceResolver usando el solicitante de la query")
  void delegatesToResolver() {

    final var resolver = mock(UserPresenceResolver.class);
    final var handler = new GetUserPresenceQueryHandler(resolver);
    final var player = PlayerId.generate();
    final var expected = UserPresenceDTO.of(new ActiveMatchRefDTO("m-1", "IN_PROGRESS"), null, null,
        null, null);
    when(resolver.resolve(player)).thenReturn(expected);

    final var presence = handler.handle(new GetUserPresenceQuery(player));

    assertThat(presence).isSameAs(expected);
    verify(resolver).resolve(player);
  }

}
