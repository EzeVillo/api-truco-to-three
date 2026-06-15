package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.events.RecordedDecisionCaptured;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.domain.ports.GameplayRecorderPort;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GameplayRecordingEventHandler implements
    ApplicationEventHandler<RecordedDecisionCaptured> {

  private static final Logger LOGGER = LoggerFactory.getLogger(GameplayRecordingEventHandler.class);

  private final GameplayRecorderPort gameplayRecorderPort;

  public GameplayRecordingEventHandler(final GameplayRecorderPort gameplayRecorderPort) {

    this.gameplayRecorderPort = Objects.requireNonNull(gameplayRecorderPort);
  }

  @Override
  public Class<RecordedDecisionCaptured> eventType() {

    return RecordedDecisionCaptured.class;
  }

  @Override
  public void handle(final RecordedDecisionCaptured event) {

    try {
      this.gameplayRecorderPort.record(event.decision());
    } catch (final RuntimeException exception) {
      LOGGER.warn("No se pudo persistir la decisión de la partida {}: {}",
          event.decision().matchId().value(), exception.getMessage(), exception);
    }
  }

}
