package com.villo.truco.infrastructure.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.villo.truco.application.events.MatchEventNotification;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@DisplayName("StompMatchNotificationHandler")
class StompMatchEventNotifierTest {

  @Test
  @DisplayName("envía broadcast a ambos jugadores")
  void broadcastsToBothPlayers() {

    final var messaging = mock(SimpMessagingTemplate.class);
    final var handler = new StompMatchNotificationHandler(messaging,
        mock(EventNotifierHealthRegistry.class));
    final var matchId = MatchId.generate();
    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();

    handler.handle(
        new MatchEventNotification(matchId, List.of(playerOne, playerTwo), "PLAYER_JOINED",
            System.currentTimeMillis(), Map.of()));

    verify(messaging, times(2)).convertAndSendToUser(any(), eq("/queue/events"), any());
  }

  @Test
  @DisplayName("envía seat-targeted solo al destinatario")
  void sendsSeatTargetedOnlyToRecipient() {

    final var messaging = mock(SimpMessagingTemplate.class);
    final var handler = new StompMatchNotificationHandler(messaging,
        mock(EventNotifierHealthRegistry.class));
    final var matchId = MatchId.generate();
    final var playerOne = PlayerId.generate();

    handler.handle(
        new MatchEventNotification(matchId, List.of(playerOne), "AVAILABLE_ACTIONS_UPDATED",
            System.currentTimeMillis(), Map.of()));

    verify(messaging, times(1)).convertAndSendToUser(any(), eq("/queue/events"), any());
  }

}
