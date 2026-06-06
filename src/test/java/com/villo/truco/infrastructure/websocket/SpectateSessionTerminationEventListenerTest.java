package com.villo.truco.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.villo.truco.application.commands.StopSpectatingMatchCommand;
import com.villo.truco.application.ports.in.StopSpectatingMatchUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@DisplayName("SpectateSessionTerminationEventListener")
class SpectateSessionTerminationEventListenerTest {

  private static Message<byte[]> unsubscribeMessage(final String sessionId,
      final String subscriptionId) {

    final var accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
    accessor.setSessionId(sessionId);
    accessor.setSubscriptionId(subscriptionId);
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  @Test
  @DisplayName("al desuscribirse deja de spectear la sesion puntual")
  void stopsSpectatingOnUnsubscribe() {

    final var useCase = mock(StopSpectatingMatchUseCase.class);
    final var registry = new SpectateSessionRegistry();
    final var listener = new SpectateSessionTerminationEventListener(useCase, registry);
    final var playerId = java.util.UUID.randomUUID().toString();

    registry.register("session-1", "sub-1", playerId, java.util.UUID.randomUUID().toString());

    listener.onUnsubscribe(
        new SessionUnsubscribeEvent(this, unsubscribeMessage("session-1", "sub-1")));

    final var commandCaptor = ArgumentCaptor.forClass(StopSpectatingMatchCommand.class);
    verify(useCase).handle(commandCaptor.capture());
    assertThat(commandCaptor.getValue().spectatorId().value().toString()).isEqualTo(playerId);
    assertThat(registry.removeSubscription("session-1", "sub-1")).isNull();
  }

  @Test
  @DisplayName("al desconectarse limpia todas las subscriptions de la sesion")
  void stopsSpectatingAllSessionSubscriptionsOnDisconnect() {

    final var useCase = mock(StopSpectatingMatchUseCase.class);
    final var registry = new SpectateSessionRegistry();
    final var listener = new SpectateSessionTerminationEventListener(useCase, registry);

    registry.register("session-1", "sub-1", java.util.UUID.randomUUID().toString(),
        java.util.UUID.randomUUID().toString());
    registry.register("session-1", "sub-2", java.util.UUID.randomUUID().toString(),
        java.util.UUID.randomUUID().toString());

    listener.onDisconnect(
        new SessionDisconnectEvent(this, unsubscribeMessage("session-1", "sub-1"), "session-1",
            CloseStatus.NORMAL));

    verify(useCase, times(2)).handle(any());
    assertThat(registry.removeSession("session-1")).isEmpty();
  }

  @Test
  @DisplayName("no detiene el especteo al desuscribirse si el jugador tiene otra sesion activa")
  void doesNotStopSpectatingOnUnsubscribeIfOtherSessionActive() {

    final var useCase = mock(StopSpectatingMatchUseCase.class);
    final var registry = new SpectateSessionRegistry();
    final var listener = new SpectateSessionTerminationEventListener(useCase, registry);
    final var playerId = java.util.UUID.randomUUID().toString();
    final var matchId = java.util.UUID.randomUUID().toString();

    registry.register("session-1", "sub-1", playerId, matchId);
    registry.register("session-2", "sub-2", playerId, matchId);

    listener.onUnsubscribe(
        new SessionUnsubscribeEvent(this, unsubscribeMessage("session-1", "sub-1")));

    verify(useCase, never()).handle(any());
  }

  @Test
  @DisplayName("no detiene el especteo al desconectarse si el jugador tiene otra sesion activa")
  void doesNotStopSpectatingOnDisconnectIfOtherSessionActive() {

    final var useCase = mock(StopSpectatingMatchUseCase.class);
    final var registry = new SpectateSessionRegistry();
    final var listener = new SpectateSessionTerminationEventListener(useCase, registry);
    final var playerId = java.util.UUID.randomUUID().toString();
    final var matchId = java.util.UUID.randomUUID().toString();

    registry.register("session-1", "sub-1", playerId, matchId);
    registry.register("session-2", "sub-2", playerId, matchId);

    listener.onDisconnect(
        new SessionDisconnectEvent(this, unsubscribeMessage("session-1", "sub-1"), "session-1",
            CloseStatus.NORMAL));

    verify(useCase, never()).handle(any());
  }

  @Test
  @DisplayName("detiene el especteo cuando se desconecta la ultima sesion del jugador")
  void stopsSpectatingWhenLastSessionDisconnects() {

    final var useCase = mock(StopSpectatingMatchUseCase.class);
    final var registry = new SpectateSessionRegistry();
    final var listener = new SpectateSessionTerminationEventListener(useCase, registry);
    final var playerId = java.util.UUID.randomUUID().toString();

    registry.register("session-1", "sub-1", playerId, java.util.UUID.randomUUID().toString());

    listener.onDisconnect(
        new SessionDisconnectEvent(this, unsubscribeMessage("session-1", "sub-1"), "session-1",
            CloseStatus.NORMAL));

    final var commandCaptor = ArgumentCaptor.forClass(StopSpectatingMatchCommand.class);
    verify(useCase).handle(commandCaptor.capture());
    assertThat(commandCaptor.getValue().spectatorId().value().toString()).isEqualTo(playerId);
  }

}
