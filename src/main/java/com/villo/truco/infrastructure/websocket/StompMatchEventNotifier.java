package com.villo.truco.infrastructure.websocket;

import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.application.ports.out.MatchEventContext;
import com.villo.truco.domain.model.match.events.SeatTargetedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.websocket.dto.MatchWsEvent;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public final class StompMatchEventNotifier implements MatchDomainEventHandler<DomainEventBase> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StompMatchEventNotifier.class);

  private final SimpMessagingTemplate messagingTemplate;

  public StompMatchEventNotifier(final SimpMessagingTemplate messagingTemplate) {

    this.messagingTemplate = Objects.requireNonNull(messagingTemplate);
  }

  @Override
  public Class<DomainEventBase> eventType() {

    return DomainEventBase.class;
  }

  @Override
  public void handle(final DomainEventBase event, final MatchEventContext context) {

    LOGGER.debug("Publishing domain event for matchId={} type={}", context.matchId(),
        event.getEventType());

    final var wsEvent = MatchWsEvent.from(event);

    if (event instanceof SeatTargetedEvent targeted) {
      final var recipient = targeted.getTargetSeat() == PlayerSeat.PLAYER_ONE ? context.playerOne()
          : context.playerTwo();
      this.sendEvent(recipient, wsEvent);
    } else {
      this.sendEvent(context.playerOne(), wsEvent);
      this.sendEvent(context.playerTwo(), wsEvent);
    }
  }

  private void sendEvent(final PlayerId playerId, final Object message) {

    final var userName = WebSocketUserNaming.userName(playerId);
    LOGGER.debug("Sending WS event to user={} type={}", userName,
        message.getClass().getSimpleName());
    this.messagingTemplate.convertAndSendToUser(userName, "/queue/events", message);
  }

}
