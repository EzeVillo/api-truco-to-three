package com.villo.truco.application.eventhandlers;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.villo.truco.application.events.MatchCompleted;
import com.villo.truco.application.ports.in.AdvanceCupUseCase;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CupMatchCompletedHandler")
class CupMatchCompletedEventHandlerTest {

  private final CupQueryRepository cupQueryRepository = mock(CupQueryRepository.class);
  private final AdvanceCupUseCase advanceCupUseCase = mock(AdvanceCupUseCase.class);
  private final CupMatchCompletedEventHandler handler = new CupMatchCompletedEventHandler(
      cupQueryRepository, advanceCupUseCase);

  @Test
  @DisplayName("delegates to AdvanceCupUseCase when cup exists for match")
  void delegatesToUseCaseWhenCupExists() {

    final var cupId = CupId.generate();
    final var matchId = MatchId.generate();
    final var winnerId = PlayerId.generate();
    final var cup = mock(Cup.class);
    when(cup.getId()).thenReturn(cupId);
    when(cupQueryRepository.findByMatchId(matchId)).thenReturn(Optional.of(cup));

    handler.handle(new MatchCompleted(matchId, winnerId));

    verify(advanceCupUseCase).handle(argThat(
        cmd -> cmd.cupId().equals(cupId) && cmd.matchId().equals(matchId) && cmd.winner()
            .equals(winnerId)));
  }

  @Test
  @DisplayName("does nothing when no cup exists for match")
  void doesNothingWhenCupNotFound() {

    final var matchId = MatchId.generate();
    when(cupQueryRepository.findByMatchId(matchId)).thenReturn(Optional.empty());

    handler.handle(new MatchCompleted(matchId, PlayerId.generate()));

    verifyNoInteractions(advanceCupUseCase);
  }

}
