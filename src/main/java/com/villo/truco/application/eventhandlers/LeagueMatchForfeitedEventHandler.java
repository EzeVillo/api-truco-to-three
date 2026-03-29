package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.commands.ForfeitLeagueCommand;
import com.villo.truco.application.events.MatchForfeited;
import com.villo.truco.application.ports.in.ForfeitLeagueUseCase;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import java.util.Objects;

public final class LeagueMatchForfeitedEventHandler implements ApplicationEventHandler<MatchForfeited> {

  private final LeagueQueryRepository leagueQueryRepository;
  private final ForfeitLeagueUseCase forfeitLeagueUseCase;

  public LeagueMatchForfeitedEventHandler(final LeagueQueryRepository leagueQueryRepository,
      final ForfeitLeagueUseCase forfeitLeagueUseCase) {

    this.leagueQueryRepository = Objects.requireNonNull(leagueQueryRepository);
    this.forfeitLeagueUseCase = Objects.requireNonNull(forfeitLeagueUseCase);
  }

  @Override
  public Class<MatchForfeited> eventType() {

    return MatchForfeited.class;
  }

  @Override
  public void handle(final MatchForfeited event) {

    this.leagueQueryRepository.findByMatchId(event.matchId()).ifPresent(
        league -> this.forfeitLeagueUseCase.handle(
            new ForfeitLeagueCommand(league.getId(), event.loserId())));
  }

}
