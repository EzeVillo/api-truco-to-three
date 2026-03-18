package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.application.ports.out.MatchEventContext;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.ports.LeagueRepository;
import java.util.Objects;

public final class LeagueMatchFinishedHandler implements
    MatchDomainEventHandler<MatchFinishedEvent> {

  private final LeagueQueryRepository leagueQueryRepository;
  private final LeagueRepository leagueRepository;

  public LeagueMatchFinishedHandler(final LeagueQueryRepository leagueQueryRepository,
      final LeagueRepository leagueRepository) {

    this.leagueQueryRepository = Objects.requireNonNull(leagueQueryRepository);
    this.leagueRepository = Objects.requireNonNull(leagueRepository);
  }

  @Override
  public Class<MatchFinishedEvent> eventType() {

    return MatchFinishedEvent.class;
  }

  @Override
  public void handle(final MatchFinishedEvent event, final MatchEventContext context) {

    final var winner =
        event.getWinnerSeat() == PlayerSeat.PLAYER_ONE ? context.playerOne() : context.playerTwo();

    this.leagueQueryRepository.findByMatchId(context.matchId()).ifPresent(league -> {
      league.recordMatchWinner(context.matchId(), winner);
      this.leagueRepository.save(league);
    });
  }

}
