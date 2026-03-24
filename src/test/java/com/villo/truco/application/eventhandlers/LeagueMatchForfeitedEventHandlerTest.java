package com.villo.truco.application.eventhandlers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.application.ports.in.ForfeitLeagueUseCase;
import com.villo.truco.application.ports.out.MatchEventContext;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LeagueMatchForfeitedHandler")
class LeagueMatchForfeitedEventHandlerTest {

  @Test
  @DisplayName("forfeitea league cuando existe")
  void forfeitsWhenLeagueExists() {

    final var query = mock(LeagueQueryRepository.class);
    final var useCase = mock(ForfeitLeagueUseCase.class);
    final var league = mock(League.class);
    when(query.findByMatchId(any())).thenReturn(Optional.of(league));
    when(league.getId()).thenReturn(LeagueId.generate());
    final var handler = new LeagueMatchForfeitedEventHandler(query, useCase);

    handler.handle(new MatchForfeitedEvent(PlayerSeat.PLAYER_ONE, 0, 0),
        new MatchEventContext(MatchId.generate(), PlayerId.generate(), PlayerId.generate()));

    verify(useCase).handle(any());
  }

  @Test
  @DisplayName("no hace nada si no hay league")
  void doesNothingWhenLeagueMissing() {

    final var query = mock(LeagueQueryRepository.class);
    final var useCase = mock(ForfeitLeagueUseCase.class);
    when(query.findByMatchId(any())).thenReturn(Optional.empty());
    final var handler = new LeagueMatchForfeitedEventHandler(query, useCase);

    handler.handle(new MatchForfeitedEvent(PlayerSeat.PLAYER_ONE, 0, 0),
        new MatchEventContext(MatchId.generate(), PlayerId.generate(), PlayerId.generate()));

    verify(useCase, never()).handle(any());
  }

}
