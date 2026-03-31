package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.commands.AdvanceCupCommand;
import com.villo.truco.application.events.MatchAbandoned;
import com.villo.truco.application.ports.in.AdvanceCupUseCase;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.domain.ports.CupQueryRepository;
import java.util.Objects;

public final class CupMatchAbandonedEventHandler implements
    ApplicationEventHandler<MatchAbandoned> {

  private final CupQueryRepository cupQueryRepository;
  private final AdvanceCupUseCase advanceCupUseCase;

  public CupMatchAbandonedEventHandler(final CupQueryRepository cupQueryRepository,
      final AdvanceCupUseCase advanceCupUseCase) {

    this.cupQueryRepository = Objects.requireNonNull(cupQueryRepository);
    this.advanceCupUseCase = Objects.requireNonNull(advanceCupUseCase);
  }

  @Override
  public Class<MatchAbandoned> eventType() {

    return MatchAbandoned.class;
  }

  @Override
  public void handle(final MatchAbandoned event) {

    this.cupQueryRepository.findByMatchId(event.matchId()).ifPresent(
        cup -> this.advanceCupUseCase.handle(
            new AdvanceCupCommand(cup.getId(), event.matchId(), event.winnerId())));
  }

}
