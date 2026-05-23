package com.villo.truco.infrastructure.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.application.ports.in.CancelQuickMatchSearchUseCase;
import java.security.Principal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@DisplayName("QuickMatchSessionDisconnectEventListener")
class QuickMatchSessionDisconnectEventListenerTest {

  private CancelQuickMatchSearchUseCase cancelUseCase;
  private QuickMatchSessionDisconnectEventListener listener;

  @BeforeEach
  void setUp() {

    cancelUseCase = mock(CancelQuickMatchSearchUseCase.class);
    listener = new QuickMatchSessionDisconnectEventListener(cancelUseCase);
  }

  private SessionDisconnectEvent eventWith(final String sessionId, final Principal principal) {

    final var accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
    accessor.setSessionId(sessionId);
    accessor.setUser(principal);
    accessor.setLeaveMutable(false);
    final var message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    return new SessionDisconnectEvent(new Object(), message, sessionId, null);
  }

  @Test
  @DisplayName("valid principal → cancel called with player id")
  void validPrincipalCallsCancel() {

    final var playerId = "11111111-1111-1111-1111-111111111111";
    final Principal principal = () -> playerId;

    listener.onDisconnect(eventWith("ws-session-1", principal));

    verify(cancelUseCase).handle(any());
  }

  @Test
  @DisplayName("null principal → cancel not called")
  void nullPrincipalDoesNotCallCancel() {

    listener.onDisconnect(eventWith("ws-session-2", null));

    verify(cancelUseCase, never()).handle(any());
  }

  @Test
  @DisplayName("cancel throws RuntimeException → exception is swallowed, no rethrow")
  void exceptionSwallowed() {

    final Principal principal = () -> "22222222-2222-2222-2222-222222222222";
    when(cancelUseCase.handle(any())).thenThrow(new RuntimeException("queue failure"));

    listener.onDisconnect(eventWith("ws-session-3", principal));
  }

}
