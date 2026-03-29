package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.commands.ForfeitCupCommand;
import com.villo.truco.application.events.MatchForfeited;
import com.villo.truco.application.ports.in.ForfeitCupUseCase;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.domain.ports.CupQueryRepository;
import java.util.Objects;

public final class CupMatchForfeitedEventHandler implements
    ApplicationEventHandler<MatchForfeited> {

  private final CupQueryRepository cupQueryRepository;
  private final ForfeitCupUseCase forfeitCupUseCase;

  public CupMatchForfeitedEventHandler(final CupQueryRepository cupQueryRepository,
      final ForfeitCupUseCase forfeitCupUseCase) {

    this.cupQueryRepository = Objects.requireNonNull(cupQueryRepository);
    this.forfeitCupUseCase = Objects.requireNonNull(forfeitCupUseCase);
  }

  @Override
  public Class<MatchForfeited> eventType() {

    return MatchForfeited.class;
  }

  @Override
  public void handle(final MatchForfeited event) {

    this.cupQueryRepository.findByMatchId(event.matchId()).ifPresent(
        cup -> this.forfeitCupUseCase.handle(new ForfeitCupCommand(cup.getId(), event.loserId())));
  }

}
