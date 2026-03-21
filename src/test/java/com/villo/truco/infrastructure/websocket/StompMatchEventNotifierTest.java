package com.villo.truco.infrastructure.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.villo.truco.application.ports.out.MatchEventContext;
import com.villo.truco.domain.model.match.events.PlayerHandUpdatedEvent;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import com.villo.truco.domain.model.match.valueobjects.Card;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.model.match.valueobjects.Suit;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@DisplayName("StompMatchEventNotifier")
class StompMatchEventNotifierTest {

  @Test
  @DisplayName("envía broadcast a ambos jugadores")
  void broadcastsToBothPlayers() {

    final var messaging = mock(SimpMessagingTemplate.class);
    final var notifier = new StompMatchEventNotifier(messaging,
      mock(EventNotifierHealthRegistry.class));
    final var context = new MatchEventContext(MatchId.generate(), PlayerId.generate(),
        PlayerId.generate());

    notifier.handle(new PlayerJoinedEvent(), context);

    verify(messaging, times(2)).convertAndSendToUser(any(), eq("/queue/events"), any());
  }

  @Test
  @DisplayName("envía seat-targeted solo al destinatario")
  void sendsSeatTargetedOnlyToRecipient() {

    final var messaging = mock(SimpMessagingTemplate.class);
    final var notifier = new StompMatchEventNotifier(messaging,
      mock(EventNotifierHealthRegistry.class));
    final var context = new MatchEventContext(MatchId.generate(), PlayerId.generate(),
        PlayerId.generate());
    final var event = new PlayerHandUpdatedEvent(PlayerSeat.PLAYER_ONE,
        List.of(Card.of(Suit.ESPADA, 1)));

    notifier.handle(event, context);

    verify(messaging, times(1)).convertAndSendToUser(any(), eq("/queue/events"), any());
  }

}
