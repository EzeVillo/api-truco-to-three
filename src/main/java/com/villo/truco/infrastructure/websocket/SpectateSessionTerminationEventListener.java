package com.villo.truco.infrastructure.websocket;

import com.villo.truco.application.commands.StopSpectatingMatchCommand;
import com.villo.truco.application.ports.in.StopSpectatingMatchUseCase;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
final class SpectateSessionTerminationEventListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      SpectateSessionTerminationEventListener.class);

  private final StopSpectatingMatchUseCase stopSpectatingMatchUseCase;
  private final SpectateSessionRegistry sessionRegistry;

  SpectateSessionTerminationEventListener(
      final StopSpectatingMatchUseCase stopSpectatingMatchUseCase,
      final SpectateSessionRegistry sessionRegistry) {

    this.stopSpectatingMatchUseCase = Objects.requireNonNull(stopSpectatingMatchUseCase);
    this.sessionRegistry = Objects.requireNonNull(sessionRegistry);
  }

  @EventListener
  public void onUnsubscribe(final SessionUnsubscribeEvent event) {

    final var accessor = StompHeaderAccessor.wrap(event.getMessage());
    final var session = this.sessionRegistry.removeSubscription(accessor.getSessionId(),
        accessor.getSubscriptionId());

    if (session != null) {
      stopSpectating(session.playerId());
    }
  }

  @EventListener
  public void onDisconnect(final SessionDisconnectEvent event) {

    this.sessionRegistry.removeSession(event.getSessionId())
        .forEach(session -> stopSpectating(session.playerId()));
  }

  private void stopSpectating(final String playerId) {

    try {
      this.stopSpectatingMatchUseCase.handle(new StopSpectatingMatchCommand(playerId));
      LOGGER.info("Spectator left: playerId={}", playerId);
    } catch (final RuntimeException ex) {
      LOGGER.warn("Stop spectating failed: playerId={}, error={}", playerId, ex.getMessage());
    }
  }

}
