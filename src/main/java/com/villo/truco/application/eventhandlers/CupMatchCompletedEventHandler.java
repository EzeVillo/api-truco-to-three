package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.commands.AdvanceCupCommand;
import com.villo.truco.application.events.MatchCompleted;
import com.villo.truco.application.ports.in.AdvanceCupUseCase;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.domain.ports.CupQueryRepository;
import java.util.Objects;

public final class CupMatchCompletedEventHandler implements
    ApplicationEventHandler<MatchCompleted> {

  private final CupQueryRepository cupQueryRepository;
  private final AdvanceCupUseCase advanceCupUseCase;

  public CupMatchCompletedEventHandler(final CupQueryRepository cupQueryRepository,
      final AdvanceCupUseCase advanceCupUseCase) {

    this.cupQueryRepository = Objects.requireNonNull(cupQueryRepository);
    this.advanceCupUseCase = Objects.requireNonNull(advanceCupUseCase);
  }

  @Override
  public Class<MatchCompleted> eventType() {

    return MatchCompleted.class;
  }

  @Override
  public void handle(final MatchCompleted event) {

    this.cupQueryRepository.findByMatchId(event.matchId()).ifPresent(
        cup -> this.advanceCupUseCase.handle(
            new AdvanceCupCommand(cup.getId(), event.matchId(), event.winnerId())));
  }

}
