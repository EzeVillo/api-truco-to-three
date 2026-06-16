package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.application.dto.UserPresenceDTO;
import com.villo.truco.application.events.PresenceEventNotification;
import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.application.usecases.queries.UserPresenceResolver;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("PresenceNotifier")
class PresenceNotifierTest {

  private UserPresenceResolver resolver;
  private ApplicationEventPublisher publisher;
  private BotRegistry botRegistry;
  private PresenceNotifier notifier;

  @BeforeEach
  void setUp() {

    resolver = mock(UserPresenceResolver.class);
    publisher = mock(ApplicationEventPublisher.class);
    botRegistry = mock(BotRegistry.class);
    notifier = new PresenceNotifier(resolver, publisher, botRegistry);
    when(botRegistry.isBot(any())).thenReturn(false);
    when(resolver.resolve(any())).thenReturn(
        UserPresenceDTO.of(null, null, null, null, null, null, null));
  }

  @Test
  @DisplayName("publica una notificacion por jugador humano con su snapshot resuelto")
  void publishesPerHumanPlayer() {

    final var player = PlayerId.generate();
    final var snapshot = UserPresenceDTO.of(null, null, null, null, null, null, null);
    when(resolver.resolve(player)).thenReturn(snapshot);

    notifier.notifyPlayers(List.of(player));

    final var captor = ArgumentCaptor.forClass(PresenceEventNotification.class);
    verify(publisher).publish(captor.capture());
    assertThat(captor.getValue().recipient()).isEqualTo(player);
    assertThat(captor.getValue().eventType()).isEqualTo(PresenceEventNotification.EVENT_TYPE);
    assertThat(captor.getValue().snapshot()).isSameAs(snapshot);
  }

  @Test
  @DisplayName("filtra bots y nulos, sin publicar para ellos")
  void filtersBotsAndNulls() {

    final var human = PlayerId.generate();
    final var bot = PlayerId.generate();
    when(botRegistry.isBot(bot)).thenReturn(true);

    notifier.notifyPlayers(Arrays.asList(human, bot, null));

    verify(publisher).publish(any(PresenceEventNotification.class));
    verify(resolver).resolve(human);
    verify(resolver, never()).resolve(bot);
  }

  @Test
  @DisplayName("deduplica jugadores repetidos en una sola notificacion")
  void deduplicatesPlayers() {

    final var player = PlayerId.generate();

    notifier.notifyPlayers(Arrays.asList(player, player));

    verify(publisher).publish(any(PresenceEventNotification.class));
  }

}
