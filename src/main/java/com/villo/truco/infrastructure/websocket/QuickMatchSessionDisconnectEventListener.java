package com.villo.truco.infrastructure.websocket;

import com.villo.truco.application.commands.CancelQuickMatchSearchCommand;
import com.villo.truco.application.ports.in.CancelQuickMatchSearchUseCase;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

public final class QuickMatchSessionDisconnectEventListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      QuickMatchSessionDisconnectEventListener.class);

  private final CancelQuickMatchSearchUseCase cancelQuickMatchSearch;

  public QuickMatchSessionDisconnectEventListener(
      final CancelQuickMatchSearchUseCase cancelQuickMatchSearch) {

    this.cancelQuickMatchSearch = Objects.requireNonNull(cancelQuickMatchSearch);
  }

  @EventListener
  public void onDisconnect(final SessionDisconnectEvent event) {

    try {
      final var accessor = StompHeaderAccessor.wrap(event.getMessage());
      final var principal = accessor.getUser();
      if (principal == null) {
        return;
      }
      final var playerId = principal.getName();
      if (playerId == null || playerId.isBlank()) {
        return;
      }
      this.cancelQuickMatchSearch.handle(
          new CancelQuickMatchSearchCommand(playerId, accessor.getSessionId()));
      LOGGER.debug("Quick match queue cleanup on disconnect: playerId={}", playerId);
    } catch (final RuntimeException ex) {
      LOGGER.warn("Quick match disconnect cleanup failed: {}", ex.getMessage());
    }
  }

}
