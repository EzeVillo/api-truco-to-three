package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.events.BotTurnRequired;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.MatchEventEnvelope;
import com.villo.truco.domain.model.match.events.RoundStartedEvent;
import com.villo.truco.domain.model.match.events.TurnChangedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import java.util.Objects;

public final class BotDomainEventTranslator implements MatchDomainEventHandler<MatchDomainEvent> {

  private final BotRegistry botRegistry;
  private final ApplicationEventPublisher applicationEventPublisher;

  public BotDomainEventTranslator(final BotRegistry botRegistry,
      final ApplicationEventPublisher applicationEventPublisher) {

    this.botRegistry = Objects.requireNonNull(botRegistry);
    this.applicationEventPublisher = Objects.requireNonNull(applicationEventPublisher);
  }

  @Override
  public Class<MatchDomainEvent> eventType() {

    return MatchDomainEvent.class;
  }

  @Override
  public void handle(final MatchDomainEvent event) {

    final var inner = event instanceof MatchEventEnvelope env ? env.getInner() : event;
    final var seat = this.resolveSeat(inner);
    if (seat == null) {
      return;
    }
    final var player = event.resolvePlayer(seat);
    if (player != null && this.botRegistry.isBot(player)) {
      this.applicationEventPublisher.publish(new BotTurnRequired(event.getMatchId(), player));
    }
  }

  private PlayerSeat resolveSeat(final Object inner) {

    if (inner instanceof TurnChangedEvent turnChangedEvent) {
      return turnChangedEvent.getSeat();
    }
    if (inner instanceof RoundStartedEvent roundStartedEvent) {
      return roundStartedEvent.getManoSeat();
    }
    return null;
  }

}
