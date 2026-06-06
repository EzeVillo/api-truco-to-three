package com.villo.truco.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.services.FriendPresenceAvailabilityNotifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@DisplayName("WebSocketSessionPresenceTracker")
class WebSocketSessionPresenceTrackerTest {

  private static final String PLAYER = "11111111-1111-1111-1111-111111111111";

  private static Message<byte[]> connectMessage(final String playerId, final String sessionId) {

    final var accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    final var sessionAttributes = new java.util.HashMap<String, Object>();
    sessionAttributes.put("authenticatedPlayer", playerId);
    accessor.setSessionAttributes(sessionAttributes);
    accessor.setSessionId(sessionId);
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private static SessionDisconnectEvent disconnectEvent(final String sessionId) {

    final var accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
    accessor.setSessionId(sessionId);
    accessor.setLeaveMutable(true);
    final var message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    return new SessionDisconnectEvent(new Object(), message, sessionId, null);
  }

  @Test
  @DisplayName("notifica a los amigos cuando el jugador pasa a online (primera sesion)")
  void notifiesWhenPlayerBecomesOnline() {

    final var notifier = mock(FriendPresenceAvailabilityNotifier.class);
    final var tracker = new WebSocketSessionPresenceTracker(notifier);

    tracker.onConnect(new SessionConnectEvent(this, connectMessage(PLAYER, "session-1")));

    assertThat(tracker.isOnline(PlayerId.of(PLAYER))).isTrue();
    verify(notifier).notifyPresenceChanged(PlayerId.of(PLAYER));
  }

  @Test
  @DisplayName("no vuelve a notificar online cuando se abre una sesion adicional")
  void doesNotRenotifyOnAdditionalSession() {

    final var notifier = mock(FriendPresenceAvailabilityNotifier.class);
    final var tracker = new WebSocketSessionPresenceTracker(notifier);

    tracker.onConnect(new SessionConnectEvent(this, connectMessage(PLAYER, "session-1")));
    tracker.onConnect(new SessionConnectEvent(this, connectMessage(PLAYER, "session-2")));

    verify(notifier).notifyPresenceChanged(PlayerId.of(PLAYER));
  }

  @Test
  @DisplayName("notifica offline solo cuando se cierra la ultima sesion")
  void notifiesOfflineOnLastSessionOnly() {

    final var notifier = mock(FriendPresenceAvailabilityNotifier.class);
    final var tracker = new WebSocketSessionPresenceTracker(notifier);
    tracker.onConnect(new SessionConnectEvent(this, connectMessage(PLAYER, "session-1")));
    tracker.onConnect(new SessionConnectEvent(this, connectMessage(PLAYER, "session-2")));

    tracker.onDisconnect(disconnectEvent("session-1"));
    assertThat(tracker.isOnline(PlayerId.of(PLAYER))).isTrue();

    tracker.onDisconnect(disconnectEvent("session-2"));
    assertThat(tracker.isOnline(PlayerId.of(PLAYER))).isFalse();

    // 1 online (primera sesion) + 1 offline (ultima desconexion); las intermedias no notifican.
    verify(notifier, org.mockito.Mockito.times(2)).notifyPresenceChanged(PlayerId.of(PLAYER));
  }

  @Test
  @DisplayName("ignora conexiones sin jugador autenticado")
  void ignoresConnectWithoutAuthenticatedPlayer() {

    final var notifier = mock(FriendPresenceAvailabilityNotifier.class);
    final var tracker = new WebSocketSessionPresenceTracker(notifier);

    final var accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.setSessionId("session-1");
    accessor.setLeaveMutable(true);
    final var message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    tracker.onConnect(new SessionConnectEvent(this, message));

    verify(notifier, never()).notifyPresenceChanged(org.mockito.ArgumentMatchers.any());
  }

}
