package com.villo.truco.infrastructure.websocket;

import com.villo.truco.domain.model.match.events.SeatTargetedEvent;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.infrastructure.websocket.dto.MatchWsEvent;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public final class StompMatchEventNotifier implements MatchEventNotifier {

  private static final Logger LOGGER = LoggerFactory.getLogger(StompMatchEventNotifier.class);

  private final SimpMessagingTemplate messagingTemplate;

  public StompMatchEventNotifier(final SimpMessagingTemplate messagingTemplate) {

    this.messagingTemplate = Objects.requireNonNull(messagingTemplate);
  }

  @Override
  public void publishDomainEvents(final MatchId matchId, final PlayerId playerOneId,
      final PlayerId playerTwoId, final List<DomainEventBase> events) {

    LOGGER.debug("Publishing {} domain events for matchId={}", events.size(), matchId);

    for (final var event : events) {
      final var wsEvent = MatchWsEvent.from(event);

      if (event instanceof SeatTargetedEvent targeted) {
        final var recipient =
            targeted.getTargetSeat() == PlayerSeat.PLAYER_ONE ? playerOneId : playerTwoId;
        this.sendEvent(matchId, recipient, wsEvent);
      } else {
        this.sendEvent(matchId, playerOneId, wsEvent);
        this.sendEvent(matchId, playerTwoId, wsEvent);
      }
    }
  }

  private void sendEvent(final MatchId matchId, final PlayerId playerId, final Object message) {

    final var userName = WebSocketUserNaming.userName(matchId, playerId);
    LOGGER.debug("Sending WS event to user={} matchId={} type={}", userName, matchId,
        message.getClass().getSimpleName());
    this.messagingTemplate.convertAndSendToUser(userName, "/queue/events", message);
  }

}
