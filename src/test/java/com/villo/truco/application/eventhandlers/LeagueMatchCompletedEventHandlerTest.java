package com.villo.truco.application.eventhandlers;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.villo.truco.application.events.MatchCompleted;
import com.villo.truco.application.ports.in.AdvanceLeagueUseCase;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LeagueMatchCompletedHandler")
class LeagueMatchCompletedEventHandlerTest {

  private final LeagueQueryRepository leagueQueryRepository = mock(LeagueQueryRepository.class);
  private final AdvanceLeagueUseCase advanceLeagueUseCase = mock(AdvanceLeagueUseCase.class);
  private final LeagueMatchCompletedEventHandler handler = new LeagueMatchCompletedEventHandler(
      leagueQueryRepository, advanceLeagueUseCase);

  @Test
  @DisplayName("delegates to AdvanceLeagueUseCase when league exists for match")
  void delegatesToUseCaseWhenLeagueExists() {

    final var leagueId = LeagueId.generate();
    final var matchId = MatchId.generate();
    final var winnerId = PlayerId.generate();
    final var league = mock(League.class);
    when(league.getId()).thenReturn(leagueId);
    when(leagueQueryRepository.findByMatchId(matchId)).thenReturn(Optional.of(league));

    handler.handle(new MatchCompleted(matchId, winnerId));

    verify(advanceLeagueUseCase).handle(argThat(
        cmd -> cmd.leagueId().equals(leagueId) && cmd.matchId().equals(matchId) && cmd.winner()
            .equals(winnerId)));
  }

  @Test
  @DisplayName("does nothing when no league exists for match")
  void doesNothingWhenLeagueNotFound() {

    final var matchId = MatchId.generate();
    when(leagueQueryRepository.findByMatchId(matchId)).thenReturn(Optional.empty());

    handler.handle(new MatchCompleted(matchId, PlayerId.generate()));

    verifyNoInteractions(advanceLeagueUseCase);
  }

}
