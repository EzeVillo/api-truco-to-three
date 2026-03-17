package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.application.ports.out.MatchEventContext;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.ports.TournamentQueryRepository;
import com.villo.truco.domain.ports.TournamentRepository;
import java.util.Objects;

public final class TournamentMatchForfeitedHandler implements
    MatchDomainEventHandler<MatchForfeitedEvent> {

  private final TournamentQueryRepository tournamentQueryRepository;
  private final TournamentRepository tournamentRepository;

  public TournamentMatchForfeitedHandler(final TournamentQueryRepository tournamentQueryRepository,
      final TournamentRepository tournamentRepository) {

    this.tournamentQueryRepository = Objects.requireNonNull(tournamentQueryRepository);
    this.tournamentRepository = Objects.requireNonNull(tournamentRepository);
  }

  @Override
  public Class<MatchForfeitedEvent> eventType() {

    return MatchForfeitedEvent.class;
  }

  @Override
  public void handle(final MatchForfeitedEvent event, final MatchEventContext context) {

    final var loser =
        event.getWinnerSeat() == PlayerSeat.PLAYER_ONE ? context.playerTwo() : context.playerOne();

    this.tournamentQueryRepository.findByMatchId(context.matchId()).ifPresent(tournament -> {
      tournament.forfeitPlayer(loser);
      this.tournamentRepository.save(tournament);
    });
  }

}
