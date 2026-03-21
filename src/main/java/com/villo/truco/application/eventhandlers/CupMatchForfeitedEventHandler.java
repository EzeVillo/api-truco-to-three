package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.commands.ForfeitCupCommand;
import com.villo.truco.application.ports.in.ForfeitCupUseCase;
import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.application.ports.out.MatchEventContext;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.ports.CupQueryRepository;
import java.util.Objects;

public final class CupMatchForfeitedEventHandler implements
    MatchDomainEventHandler<MatchForfeitedEvent> {

  private final CupQueryRepository cupQueryRepository;
  private final ForfeitCupUseCase forfeitCupUseCase;

  public CupMatchForfeitedEventHandler(final CupQueryRepository cupQueryRepository,
      final ForfeitCupUseCase forfeitCupUseCase) {

    this.cupQueryRepository = Objects.requireNonNull(cupQueryRepository);
    this.forfeitCupUseCase = Objects.requireNonNull(forfeitCupUseCase);
  }

  @Override
  public Class<MatchForfeitedEvent> eventType() {

    return MatchForfeitedEvent.class;
  }

  @Override
  public void handle(final MatchForfeitedEvent event, final MatchEventContext context) {

    final var loser =
        event.getWinnerSeat() == PlayerSeat.PLAYER_ONE ? context.playerTwo() : context.playerOne();

    this.cupQueryRepository.findByMatchId(context.matchId())
        .ifPresent(cup -> this.forfeitCupUseCase.handle(new ForfeitCupCommand(cup.getId(), loser)));
  }

}
