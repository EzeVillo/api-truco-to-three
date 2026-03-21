package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.commands.AdvanceCupCommand;
import com.villo.truco.application.ports.in.AdvanceCupUseCase;
import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.application.ports.out.MatchEventContext;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.ports.CupQueryRepository;
import java.util.Objects;

public final class CupMatchFinishedEventHandler implements
    MatchDomainEventHandler<MatchFinishedEvent> {

  private final CupQueryRepository cupQueryRepository;
  private final AdvanceCupUseCase advanceCupUseCase;

  public CupMatchFinishedEventHandler(final CupQueryRepository cupQueryRepository,
      final AdvanceCupUseCase advanceCupUseCase) {

    this.cupQueryRepository = Objects.requireNonNull(cupQueryRepository);
    this.advanceCupUseCase = Objects.requireNonNull(advanceCupUseCase);
  }

  @Override
  public Class<MatchFinishedEvent> eventType() {

    return MatchFinishedEvent.class;
  }

  @Override
  public void handle(final MatchFinishedEvent event, final MatchEventContext context) {

    final var winner =
        event.getWinnerSeat() == PlayerSeat.PLAYER_ONE ? context.playerOne() : context.playerTwo();

    this.cupQueryRepository.findByMatchId(context.matchId()).ifPresent(
        cup -> this.advanceCupUseCase.handle(
            new AdvanceCupCommand(cup.getId(), context.matchId(), winner)));
  }

}
