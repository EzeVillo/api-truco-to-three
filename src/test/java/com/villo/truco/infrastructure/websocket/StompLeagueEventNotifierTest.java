package com.villo.truco.infrastructure.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.villo.truco.application.events.LeagueEventNotification;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@DisplayName("StompLeagueNotificationHandler")
class StompLeagueEventNotifierTest {

  @Test
  @DisplayName("envía el evento a todos los participantes")
  void broadcastsToAllParticipants() {

    final var messaging = mock(SimpMessagingTemplate.class);
    final var handler = new StompLeagueNotificationHandler(messaging,
        mock(EventNotifierHealthRegistry.class));
    final var leagueId = LeagueId.generate();
    final var participants = List.of(PlayerId.generate(), PlayerId.generate(), PlayerId.generate());

    handler.handle(new LeagueEventNotification(leagueId, participants, "LEAGUE_STARTED",
        System.currentTimeMillis(), Map.of()));

    verify(messaging, times(3)).convertAndSendToUser(any(), eq("/queue/events"), any());
  }

}
