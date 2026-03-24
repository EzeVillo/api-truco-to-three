package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.commands.AdvanceLeagueCommand;
import com.villo.truco.application.ports.in.AdvanceLeagueUseCase;
import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.application.ports.out.MatchEventContext;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import java.util.Objects;

public final class LeagueMatchFinishedEventHandler implements
    MatchDomainEventHandler<MatchFinishedEvent> {

  private final LeagueQueryRepository leagueQueryRepository;
  private final AdvanceLeagueUseCase advanceLeagueUseCase;

  public LeagueMatchFinishedEventHandler(final LeagueQueryRepository leagueQueryRepository,
      final AdvanceLeagueUseCase advanceLeagueUseCase) {

    this.leagueQueryRepository = Objects.requireNonNull(leagueQueryRepository);
    this.advanceLeagueUseCase = Objects.requireNonNull(advanceLeagueUseCase);
  }

  @Override
  public Class<MatchFinishedEvent> eventType() {

    return MatchFinishedEvent.class;
  }

  @Override
  public void handle(final MatchFinishedEvent event, final MatchEventContext context) {

    final var winner =
        event.getWinnerSeat() == PlayerSeat.PLAYER_ONE ? context.playerOne() : context.playerTwo();

    this.leagueQueryRepository.findByMatchId(context.matchId()).ifPresent(
        league -> this.advanceLeagueUseCase.handle(
            new AdvanceLeagueCommand(league.getId(), context.matchId(), winner)));
  }

}
