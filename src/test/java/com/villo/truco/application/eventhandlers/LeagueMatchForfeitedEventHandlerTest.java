package com.villo.truco.application.eventhandlers;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.villo.truco.application.events.MatchForfeited;
import com.villo.truco.application.ports.in.ForfeitLeagueUseCase;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LeagueMatchForfeitedHandler")
class LeagueMatchForfeitedEventHandlerTest {

  private final LeagueQueryRepository leagueQueryRepository = mock(LeagueQueryRepository.class);
  private final ForfeitLeagueUseCase forfeitLeagueUseCase = mock(ForfeitLeagueUseCase.class);
  private final LeagueMatchForfeitedEventHandler handler = new LeagueMatchForfeitedEventHandler(
      leagueQueryRepository, forfeitLeagueUseCase);

  @Test
  @DisplayName("delegates to ForfeitLeagueUseCase with loser when league exists for match")
  void delegatesToUseCaseWhenLeagueExists() {

    final var leagueId = LeagueId.generate();
    final var matchId = MatchId.generate();
    final var winnerId = PlayerId.generate();
    final var loserId = PlayerId.generate();
    final var league = mock(League.class);
    when(league.getId()).thenReturn(leagueId);
    when(leagueQueryRepository.findByMatchId(matchId)).thenReturn(Optional.of(league));

    handler.handle(new MatchForfeited(matchId, winnerId, loserId));

    verify(forfeitLeagueUseCase).handle(
        argThat(cmd -> cmd.leagueId().equals(leagueId) && cmd.forfeiter().equals(loserId)));
  }

  @Test
  @DisplayName("does nothing when no league exists for match")
  void doesNothingWhenLeagueNotFound() {

    final var matchId = MatchId.generate();
    when(leagueQueryRepository.findByMatchId(matchId)).thenReturn(Optional.empty());

    handler.handle(new MatchForfeited(matchId, PlayerId.generate(), PlayerId.generate()));

    verifyNoInteractions(forfeitLeagueUseCase);
  }

}
