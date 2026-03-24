package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.commands.ForfeitLeagueCommand;
import com.villo.truco.application.ports.in.ForfeitLeagueUseCase;
import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.application.ports.out.MatchEventContext;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import java.util.Objects;

public final class LeagueMatchForfeitedEventHandler implements
    MatchDomainEventHandler<MatchForfeitedEvent> {

  private final LeagueQueryRepository leagueQueryRepository;
  private final ForfeitLeagueUseCase forfeitLeagueUseCase;

  public LeagueMatchForfeitedEventHandler(final LeagueQueryRepository leagueQueryRepository,
      final ForfeitLeagueUseCase forfeitLeagueUseCase) {

    this.leagueQueryRepository = Objects.requireNonNull(leagueQueryRepository);
    this.forfeitLeagueUseCase = Objects.requireNonNull(forfeitLeagueUseCase);
  }

  @Override
  public Class<MatchForfeitedEvent> eventType() {

    return MatchForfeitedEvent.class;
  }

  @Override
  public void handle(final MatchForfeitedEvent event, final MatchEventContext context) {

    final var loser =
        event.getWinnerSeat() == PlayerSeat.PLAYER_ONE ? context.playerTwo() : context.playerOne();

    this.leagueQueryRepository.findByMatchId(context.matchId()).ifPresent(
        league -> this.forfeitLeagueUseCase.handle(
            new ForfeitLeagueCommand(league.getId(), loser)));
  }

}
