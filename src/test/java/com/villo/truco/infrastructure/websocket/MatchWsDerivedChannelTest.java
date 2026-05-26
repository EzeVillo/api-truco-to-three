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
import com.villo.truco.infrastructure.websocket.dto.MatchDerivedWsEvent;
import com.villo.truco.infrastructure.websocket.dto.MatchWsEvent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@DisplayName("Match WS derived channel")
class MatchWsDerivedChannelTest {

  @Test
  @DisplayName("Eventos derivados se envían a /user/queue/match-derived")
  void derivedEventsGoToMatchDerivedQueue() {

    final var messaging = mock(SimpMessagingTemplate.class);
    final var handler = new StompMatchNotificationHandler(messaging,
        mock(EventNotifierHealthRegistry.class));
    final var matchId = MatchId.generate();
    final var playerOne = PlayerId.generate();

    handler.handle(new MatchEventNotification(matchId, List.of(playerOne), "PLAYER_HAND_UPDATED",
        System.currentTimeMillis(), Map.of(), null));

    verify(messaging, times(1)).convertAndSendToUser(any(), eq("/queue/match-derived"), any());
  }

  @Test
  @DisplayName("Eventos derivados publican MatchDerivedWsEvent")
  void derivedEventsPublishMatchDerivedWsEvent() {

    final var messaging = mock(SimpMessagingTemplate.class);
    final var handler = new StompMatchNotificationHandler(messaging,
        mock(EventNotifierHealthRegistry.class));
    final var matchId = MatchId.generate();
    final var playerOne = PlayerId.generate();

    handler.handle(
        new MatchEventNotification(matchId, List.of(playerOne), "AVAILABLE_ACTIONS_UPDATED",
            System.currentTimeMillis(), Map.of(), null));

    final var captor = ArgumentCaptor.forClass(MatchDerivedWsEvent.class);
    verify(messaging, times(1)).convertAndSendToUser(any(), eq("/queue/match-derived"),
        captor.capture());

    final var sentEvent = captor.getValue();
    assertThat(sentEvent).isInstanceOf(MatchDerivedWsEvent.class);
  }

  @Test
  @DisplayName("Eventos transicionales se envían a /user/queue/match con stateVersion")
  void transitionalEventsGoToMatchQueueWithStateVersion() {

    final var messaging = mock(SimpMessagingTemplate.class);
    final var handler = new StompMatchNotificationHandler(messaging,
        mock(EventNotifierHealthRegistry.class));
    final var matchId = MatchId.generate();
    final var playerOne = PlayerId.generate();

    handler.handle(new MatchEventNotification(matchId, List.of(playerOne), "CARD_PLAYED",
        System.currentTimeMillis(), Map.of(), 7L));

    final var captor = ArgumentCaptor.forClass(MatchWsEvent.class);
    verify(messaging, times(1)).convertAndSendToUser(any(), eq("/queue/match"), captor.capture());

    final var sentEvent = captor.getValue();
    assertThat(sentEvent.stateVersion()).isEqualTo(7L);
  }

}
