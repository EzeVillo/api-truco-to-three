package com.villo.truco.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.villo.truco.application.events.MatchEventNotification;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.websocket.dto.MatchWsEvent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@DisplayName("Match WS reconnection")
class MatchWsReconnectionTest {

  @Test
  @DisplayName("El cliente detecta hueco por salto en stateVersion")
  void clientDetectsGapByStateVersionJump() {

    final var messaging = mock(SimpMessagingTemplate.class);
    final var handler = new StompMatchNotificationHandler(messaging,
        mock(EventNotifierHealthRegistry.class));
    final var matchId = MatchId.generate();
    final var playerOne = PlayerId.generate();

    handler.handle(new MatchEventNotification(matchId, List.of(playerOne), "PLAYER_JOINED",
        System.currentTimeMillis(), Map.of(), 1L));

    handler.handle(new MatchEventNotification(matchId, List.of(playerOne), "GAME_STARTED",
        System.currentTimeMillis(), Map.of(), 3L));

    final var captor = ArgumentCaptor.forClass(MatchWsEvent.class);
    verify(messaging, times(2)).convertAndSendToUser(any(), eq("/queue/match"), captor.capture());

    final var events = captor.getAllValues();
    assertThat(events).hasSize(2);
    assertThat(events.get(0).stateVersion()).isEqualTo(1L);
    assertThat(events.get(1).stateVersion()).isEqualTo(3L);

    final var lastApplied = events.get(0).stateVersion();
    final var nextReceived = events.get(1).stateVersion();
    assertThat(nextReceived).isGreaterThan(lastApplied + 1);
  }

}
