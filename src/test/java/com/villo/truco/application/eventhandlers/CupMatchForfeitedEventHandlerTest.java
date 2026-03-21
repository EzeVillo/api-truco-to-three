package com.villo.truco.application.eventhandlers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.application.ports.in.ForfeitCupUseCase;
import com.villo.truco.application.ports.out.MatchEventContext;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CupMatchForfeitedEventHandler")
class CupMatchForfeitedEventHandlerTest {

  @Test
  @DisplayName("ejecuta forfeit cuando existe cup")
  void callsForfeitWhenCupExists() {

    final var query = mock(CupQueryRepository.class);
    final var useCase = mock(ForfeitCupUseCase.class);
    final var cup = mock(Cup.class);
    when(query.findByMatchId(any())).thenReturn(Optional.of(cup));
    when(cup.getId()).thenReturn(com.villo.truco.domain.model.cup.valueobjects.CupId.generate());
    final var handler = new CupMatchForfeitedEventHandler(query, useCase);

    handler.handle(new MatchForfeitedEvent(PlayerSeat.PLAYER_ONE, 1, 0),
        new MatchEventContext(MatchId.generate(), PlayerId.generate(), PlayerId.generate()));

    verify(useCase).handle(any());
  }

}
