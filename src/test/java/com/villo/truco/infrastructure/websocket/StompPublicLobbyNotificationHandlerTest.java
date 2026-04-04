package com.villo.truco.infrastructure.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.villo.truco.application.events.PublicCupLobbyNotification;
import com.villo.truco.application.events.PublicLeagueLobbyNotification;
import com.villo.truco.application.events.PublicMatchLobbyNotification;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@DisplayName("Public lobby STOMP handlers")
class StompPublicLobbyNotificationHandlerTest {

  @Test
  @DisplayName("envia eventos de match al topic compartido correcto")
  void sendsMatchEventsToCorrectDestination() {

    final var messaging = mock(SimpMessagingTemplate.class);
    final var handler = new StompPublicMatchLobbyNotificationHandler(messaging,
        mock(EventNotifierHealthRegistry.class));

    handler.handle(
        new PublicMatchLobbyNotification("PUBLIC_MATCH_LOBBY_UPSERT", System.currentTimeMillis(),
            Map.of("id", "match-id")));

    verify(messaging).convertAndSend(eq("/topic/public-match-lobby"), any(Object.class));
  }

  @Test
  @DisplayName("envia eventos de cup al topic compartido correcto")
  void sendsCupEventsToCorrectDestination() {

    final var messaging = mock(SimpMessagingTemplate.class);
    final var handler = new StompPublicCupLobbyNotificationHandler(messaging,
        mock(EventNotifierHealthRegistry.class));

    handler.handle(
        new PublicCupLobbyNotification("PUBLIC_CUP_LOBBY_REMOVED", System.currentTimeMillis(),
            Map.of("id", "cup-id")));

    verify(messaging).convertAndSend(eq("/topic/public-cup-lobby"), any(Object.class));
  }

  @Test
  @DisplayName("envia eventos de league al topic compartido correcto")
  void sendsLeagueEventsToCorrectDestination() {

    final var messaging = mock(SimpMessagingTemplate.class);
    final var handler = new StompPublicLeagueLobbyNotificationHandler(messaging,
        mock(EventNotifierHealthRegistry.class));

    handler.handle(
        new PublicLeagueLobbyNotification("PUBLIC_LEAGUE_LOBBY_REMOVED", System.currentTimeMillis(),
            Map.of("id", "league-id")));

    verify(messaging).convertAndSend(eq("/topic/public-league-lobby"), any(Object.class));
  }

}
