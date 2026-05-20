package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.events.MatchEventNotification;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.domain.model.rematch.events.RematchSessionOpenedEvent;
import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionId;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RematchNotificationEventTranslator")
class RematchNotificationEventTranslatorTest {

  private final List<ApplicationEvent> published = new ArrayList<>();
  private final ApplicationEventPublisher publisher = published::add;
  private final RematchNotificationEventTranslator translator = new RematchNotificationEventTranslator(
      publisher, publicActorResolver);
  private final PublicActorResolver publicActorResolver = mock(PublicActorResolver.class);

  {
    when(publicActorResolver.resolve(any())).thenReturn("actor");
  }

  @Test
  @DisplayName("handleOpened sends notification only to human when bot is player two")
  void openedExcludesBotPlayerTwo() {

    final var human = PlayerId.generate();
    final var bot = PlayerId.generate();
    final var event = new RematchSessionOpenedEvent(RematchSessionId.generate(), MatchId.generate(),
        human, bot, Instant.now(), false, true);

    translator.handle(event);

    assertThat(published).hasSize(1);
    final var notification = (MatchEventNotification) published.getFirst();
    assertThat(notification.recipients()).containsExactly(human);
    assertThat(notification.recipients()).doesNotContain(bot);
  }

  @Test
  @DisplayName("handleOpened sends notification only to human when bot is player one")
  void openedExcludesBotPlayerOne() {

    final var bot = PlayerId.generate();
    final var human = PlayerId.generate();
    final var event = new RematchSessionOpenedEvent(RematchSessionId.generate(), MatchId.generate(),
        bot, human, Instant.now(), true, false);

    translator.handle(event);

    assertThat(published).hasSize(1);
    final var notification = (MatchEventNotification) published.getFirst();
    assertThat(notification.recipients()).containsExactly(human);
    assertThat(notification.recipients()).doesNotContain(bot);
  }

  @Test
  @DisplayName("handleOpened sends notification to both players when neither is a bot")
  void openedSendsToAllHumans() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    final var event = new RematchSessionOpenedEvent(RematchSessionId.generate(), MatchId.generate(),
        playerOne, playerTwo, Instant.now(), false, false);

    translator.handle(event);

    assertThat(published).hasSize(1);
    final var notification = (MatchEventNotification) published.getFirst();
    assertThat(notification.recipients()).containsExactlyInAnyOrder(playerOne, playerTwo);
  }

}
