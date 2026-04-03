package com.villo.truco.infrastructure.websocket;

import com.villo.truco.application.events.SpectatorCountChanged;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.websocket.dto.MatchWsEvent;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public final class StompSpectatorCountHandler implements
    ApplicationEventHandler<SpectatorCountChanged> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StompSpectatorCountHandler.class);

  private final SimpMessagingTemplate messagingTemplate;
  private final EventNotifierHealthRegistry healthRegistry;

  public StompSpectatorCountHandler(final SimpMessagingTemplate messagingTemplate,
      final EventNotifierHealthRegistry healthRegistry) {

    this.messagingTemplate = Objects.requireNonNull(messagingTemplate);
    this.healthRegistry = Objects.requireNonNull(healthRegistry);
  }

  @Override
  public Class<SpectatorCountChanged> eventType() {

    return SpectatorCountChanged.class;
  }

  @Override
  public void handle(final SpectatorCountChanged event) {

    LOGGER.debug("Publishing spectator count changed matchId={} count={}", event.matchId(),
        event.count());

    final var wsEvent = new MatchWsEvent(event.matchId().value().toString(),
        "SPECTATOR_COUNT_CHANGED", System.currentTimeMillis(),
        Map.of("spectatorCount", event.count()));

    for (final var playerId : event.players()) {
      this.sendEvent(playerId, "/queue/match", wsEvent);
    }

    for (final var spectatorId : event.spectators()) {
      this.sendEvent(spectatorId, "/queue/match-spectate", wsEvent);
    }
  }

  private void sendEvent(final PlayerId playerId, final String destination, final Object message) {

    final var userName = WebSocketUserNaming.userName(playerId);
    try {
      this.messagingTemplate.convertAndSendToUser(userName, destination, message);
      this.healthRegistry.recordSuccess();
    } catch (final RuntimeException ex) {
      this.healthRegistry.recordFailure(ex);
      throw ex;
    }
  }

}
