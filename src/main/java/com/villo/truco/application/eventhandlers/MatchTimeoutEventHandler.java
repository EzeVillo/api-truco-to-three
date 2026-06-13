package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutScheduler;
import com.villo.truco.application.timeout.MatchTimeoutPhasePolicy;
import com.villo.truco.domain.model.match.events.ActionDeadlineClearedEvent;
import com.villo.truco.domain.model.match.events.ActionDeadlineSetEvent;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.MatchEventEnvelope;
import java.time.Duration;
import java.util.Objects;

public class MatchTimeoutEventHandler extends AbstractTimeoutEventHandler implements
    MatchDomainEventHandler<MatchDomainEvent> {

  private final MatchTimeoutPhasePolicy phasePolicy;
  private final Duration lobbyTimeout;
  private final Duration playTimeout;

  public MatchTimeoutEventHandler(final TimeoutScheduler timeoutScheduler,
      final TimeoutActionDispatcher dispatcher, final MatchTimeoutPhasePolicy phasePolicy,
      final Duration lobbyTimeout, final Duration playTimeout) {

    super(timeoutScheduler, dispatcher);
    this.phasePolicy = Objects.requireNonNull(phasePolicy);
    this.lobbyTimeout = Objects.requireNonNull(lobbyTimeout);
    this.playTimeout = Objects.requireNonNull(playTimeout);
  }

  @Override
  public Class<MatchDomainEvent> eventType() {

    return MatchDomainEvent.class;
  }

  @Override
  public void handle(final MatchDomainEvent event) {

    final var inner = event instanceof MatchEventEnvelope env ? env.getInner() : event;
    final var matchId = event.getMatchId().value().toString();

    // Los eventos de deadline son proyecciones derivadas, no transiciones de actividad: no deben
    // (re)programar ni cancelar el reloj. Los eventos transicionales reales ya lo manejan, y
    // ActionDeadlineCleared se emite despues de MatchFinished, por lo que reprogramar aqui dejaria
    // un timeout colgado sobre un match terminado.
    if (inner instanceof ActionDeadlineSetEvent || inner instanceof ActionDeadlineClearedEvent) {
      return;
    }

    final var phase = this.phasePolicy.phaseOf(inner);
    switch (phase) {
      case NONE -> cancelTimeout(EntityType.MATCH, matchId);
      case LOBBY -> scheduleTimeoutFromNow(EntityType.MATCH, matchId, this.lobbyTimeout);
      case PLAY -> scheduleTimeoutFromNow(EntityType.MATCH, matchId, this.playTimeout);
    }
  }

}
