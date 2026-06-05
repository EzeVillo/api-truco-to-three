package com.villo.truco.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.application.dto.ActiveMatchRefDTO;
import com.villo.truco.application.dto.UserPresenceDTO;
import com.villo.truco.application.eventhandlers.MatchPresenceEventTranslator;
import com.villo.truco.application.eventhandlers.PresenceNotifier;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.out.ApplicationEventHandler;
import com.villo.truco.application.usecases.queries.UserPresenceResolver;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.actuator.health.EventNotifierHealthRegistry;
import com.villo.truco.infrastructure.events.InProcessApplicationEventPublisher;
import com.villo.truco.infrastructure.websocket.dto.PresenceWsEvent;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Integracion del path de presencia sin broker: un domain event de ingreso a partida atraviesa el
 * traductor -> PresenceNotifier -> publisher de eventos de aplicacion -> handler STOMP, y verifica
 * que el snapshot llega a la cola de ambos jugadores y no a la de un tercero (aislamiento,
 * FR-008).
 */
@DisplayName("Presence push (integracion del path de publicacion)")
class PresencePushIntegrationTest {

  @Test
  @DisplayName("el ingreso a una partida empuja el snapshot a las colas de ambos jugadores y no a terceros")
  void pushesSnapshotToBothPlayersOnly() {

    final var template = mock(SimpMessagingTemplate.class);
    final var botRegistry = mock(BotRegistry.class);
    final var resolver = mock(UserPresenceResolver.class);
    when(botRegistry.isBot(any())).thenReturn(false);

    final var playerOne = PlayerId.of("11111111-1111-1111-1111-111111111111");
    final var playerTwo = PlayerId.of("22222222-2222-2222-2222-222222222222");
    final var snapshot = UserPresenceDTO.of(new ActiveMatchRefDTO("m-1", "READY"), null, null,
        null);
    when(resolver.resolve(any())).thenReturn(snapshot);

    final var stompHandler = new StompPresenceNotificationHandler(template,
        mock(EventNotifierHealthRegistry.class));
    final List<ApplicationEventHandler<?>> handlers = List.of(stompHandler);
    final var publisher = new InProcessApplicationEventPublisher(handlers);
    final var notifier = new PresenceNotifier(resolver, publisher, botRegistry);
    final var translator = new MatchPresenceEventTranslator(notifier);

    translator.handle(new PlayerJoinedEvent(MatchId.generate(), playerOne, playerTwo));

    final var payloadCaptor = ArgumentCaptor.forClass(PresenceWsEvent.class);
    verify(template).convertAndSendToUser(eq("11111111-1111-1111-1111-111111111111"),
        eq("/queue/presence"), payloadCaptor.capture());
    verify(template).convertAndSendToUser(eq("22222222-2222-2222-2222-222222222222"),
        eq("/queue/presence"), any(PresenceWsEvent.class));
    verify(template, never()).convertAndSendToUser(eq("33333333-3333-3333-3333-333333333333"),
        eq("/queue/presence"), any());
    assertThat(payloadCaptor.getValue().payload().match().id()).isEqualTo("m-1");
  }

}
