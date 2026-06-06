package com.villo.truco.social.infrastructure.websocket;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.websocket.WebSocketUserNaming;
import com.villo.truco.social.application.ports.in.GetFriendActivityUseCase;
import com.villo.truco.social.application.ports.in.GetFriendAvailabilityUseCase;
import com.villo.truco.social.application.queries.GetFriendActivityQuery;
import com.villo.truco.social.application.queries.GetFriendAvailabilityQuery;
import com.villo.truco.social.infrastructure.websocket.dto.SocialWsEvent;
import java.util.LinkedHashMap;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
final class SocialSubscribeEventListener {

  static final String STATE_EVENT_TYPE = "FRIEND_ACTIVITY_STATE";
  static final String AVAILABILITY_STATE_EVENT_TYPE = "FRIEND_AVAILABILITY_STATE";

  private static final Logger LOGGER = LoggerFactory.getLogger(SocialSubscribeEventListener.class);
  private static final String SOCIAL_DESTINATION = "/user/queue/social";
  private static final String IDENTITY_ATTR = "authenticatedPlayer";

  private final GetFriendActivityUseCase getFriendActivityUseCase;
  private final GetFriendAvailabilityUseCase getFriendAvailabilityUseCase;
  private final SimpMessagingTemplate messagingTemplate;

  SocialSubscribeEventListener(final GetFriendActivityUseCase getFriendActivityUseCase,
      final GetFriendAvailabilityUseCase getFriendAvailabilityUseCase,
      final SimpMessagingTemplate messagingTemplate) {

    this.getFriendActivityUseCase = Objects.requireNonNull(getFriendActivityUseCase);
    this.getFriendAvailabilityUseCase = Objects.requireNonNull(getFriendAvailabilityUseCase);
    this.messagingTemplate = Objects.requireNonNull(messagingTemplate);
  }

  private static String extractAuthenticatedPlayer(final StompHeaderAccessor accessor) {

    final var attrs = accessor.getSessionAttributes();
    if (attrs == null) {
      return null;
    }

    return (String) attrs.get(IDENTITY_ATTR);
  }

  @EventListener
  public void onSubscribe(final SessionSubscribeEvent event) {

    final var accessor = StompHeaderAccessor.wrap(event.getMessage());
    if (!SOCIAL_DESTINATION.equals(accessor.getDestination())) {
      return;
    }

    final var playerId = extractAuthenticatedPlayer(accessor);
    if (playerId == null) {
      LOGGER.warn("Social subscribe ignored: missing authenticated player");
      return;
    }

    final var state = this.getFriendActivityUseCase.handle(
        new GetFriendActivityQuery(PlayerId.of(playerId)));
    final var payload = new LinkedHashMap<String, Object>();
    payload.put("friends", state.friends());
    final var wsEvent = new SocialWsEvent(STATE_EVENT_TYPE, System.currentTimeMillis(), payload);

    this.messagingTemplate.convertAndSendToUser(WebSocketUserNaming.userName(playerId),
        "/queue/social", wsEvent);

    final var availabilityState = this.getFriendAvailabilityUseCase.handle(
        new GetFriendAvailabilityQuery(PlayerId.of(playerId)));
    final var availabilityPayload = new LinkedHashMap<String, Object>();
    availabilityPayload.put("friends", availabilityState.friends());
    final var availabilityWsEvent = new SocialWsEvent(AVAILABILITY_STATE_EVENT_TYPE,
        System.currentTimeMillis(), availabilityPayload);
    this.messagingTemplate.convertAndSendToUser(WebSocketUserNaming.userName(playerId),
        "/queue/social", availabilityWsEvent);
  }

}
