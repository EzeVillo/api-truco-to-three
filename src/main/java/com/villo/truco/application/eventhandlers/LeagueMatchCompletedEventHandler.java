package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.commands.AdvanceLeagueCommand;
import com.villo.truco.application.events.MatchCompleted;
import com.villo.truco.application.ports.in.AdvanceLeagueUseCase;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import java.util.Objects;

public final class LeagueMatchCompletedEventHandler implements
    ApplicationEventHandler<MatchCompleted> {

  private final LeagueQueryRepository leagueQueryRepository;
  private final AdvanceLeagueUseCase advanceLeagueUseCase;

  public LeagueMatchCompletedEventHandler(final LeagueQueryRepository leagueQueryRepository,
      final AdvanceLeagueUseCase advanceLeagueUseCase) {

    this.leagueQueryRepository = Objects.requireNonNull(leagueQueryRepository);
    this.advanceLeagueUseCase = Objects.requireNonNull(advanceLeagueUseCase);
  }

  @Override
  public Class<MatchCompleted> eventType() {

    return MatchCompleted.class;
  }

  @Override
  public void handle(final MatchCompleted event) {

    this.leagueQueryRepository.findByMatchId(event.matchId()).ifPresent(
        league -> this.advanceLeagueUseCase.handle(
            new AdvanceLeagueCommand(league.getId(), event.matchId(), event.winnerId())));
  }

}
