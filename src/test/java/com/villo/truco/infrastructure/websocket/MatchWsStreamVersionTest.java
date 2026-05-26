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

@DisplayName("Match WS stream version")
class MatchWsStreamVersionTest {

  @Test
  @DisplayName("Los eventos transicionales incluyen stateVersion en el WS event")
  void transitionalEventIncludesStateVersion() {

    final var messaging = mock(SimpMessagingTemplate.class);
    final var handler = new StompMatchNotificationHandler(messaging,
        mock(EventNotifierHealthRegistry.class));
    final var matchId = MatchId.generate();
    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();

    handler.handle(
        new MatchEventNotification(matchId, List.of(playerOne, playerTwo), "PLAYER_JOINED",
            System.currentTimeMillis(), Map.of(), 5L));

    final var captor = ArgumentCaptor.forClass(MatchWsEvent.class);
    verify(messaging, times(2)).convertAndSendToUser(any(), eq("/queue/match"), captor.capture());

    final var sentEvents = captor.getAllValues();
    assertThat(sentEvents).hasSize(2);
    for (final var event : sentEvents) {
      assertThat(event.stateVersion()).isEqualTo(5L);
    }
  }

  @Test
  @DisplayName("Los eventos derivados publican stateVersion null")
  void derivedEventPublishesNullStateVersion() {

    final var messaging = mock(SimpMessagingTemplate.class);
    final var handler = new StompMatchNotificationHandler(messaging,
        mock(EventNotifierHealthRegistry.class));
    final var matchId = MatchId.generate();
    final var playerOne = PlayerId.generate();

    handler.handle(
        new MatchEventNotification(matchId, List.of(playerOne), "AVAILABLE_ACTIONS_UPDATED",
            System.currentTimeMillis(), Map.of(), null));

    final var captor = ArgumentCaptor.forClass(MatchWsEvent.class);
    verify(messaging, times(1)).convertAndSendToUser(any(), eq("/queue/match-derived"),
        captor.capture());

    final var sentEvent = captor.getValue();
    assertThat(sentEvent.stateVersion()).isNull();
  }

}
