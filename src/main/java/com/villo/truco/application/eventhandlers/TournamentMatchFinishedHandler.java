package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.application.ports.out.MatchEventContext;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.ports.TournamentQueryRepository;
import com.villo.truco.domain.ports.TournamentRepository;
import java.util.Objects;

public final class TournamentMatchFinishedHandler implements
    MatchDomainEventHandler<MatchFinishedEvent> {

  private final TournamentQueryRepository tournamentQueryRepository;
  private final TournamentRepository tournamentRepository;

  public TournamentMatchFinishedHandler(final TournamentQueryRepository tournamentQueryRepository,
      final TournamentRepository tournamentRepository) {

    this.tournamentQueryRepository = Objects.requireNonNull(tournamentQueryRepository);
    this.tournamentRepository = Objects.requireNonNull(tournamentRepository);
  }

  @Override
  public Class<MatchFinishedEvent> eventType() {

    return MatchFinishedEvent.class;
  }

  @Override
  public void handle(final MatchFinishedEvent event, final MatchEventContext context) {

    final var winner =
        event.getWinnerSeat() == PlayerSeat.PLAYER_ONE ? context.playerOne() : context.playerTwo();

    this.tournamentQueryRepository.findByMatchId(context.matchId()).ifPresent(tournament -> {
      tournament.recordMatchWinner(context.matchId(), winner);
      this.tournamentRepository.save(tournament);
    });
  }

}
