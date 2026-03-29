package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.events.CupEventNotification;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import com.villo.truco.domain.model.cup.events.CupStartedEvent;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CupNotificationTranslator")
class CupNotificationEventTranslatorTest {

  private final List<ApplicationEvent> published = new ArrayList<>();
  private final ApplicationEventPublisher publisher = published::add;
  private final CupNotificationEventTranslator translator = new CupNotificationEventTranslator(
      new CupEventMapper(), publisher);

  @Test
  @DisplayName("CupStartedEvent → publica CupEventNotification con todos los participantes")
  void cupStartedPublishesNotificationWithAllParticipants() {

    final var cupId = CupId.generate();
    final var participants = List.of(PlayerId.generate(), PlayerId.generate(), PlayerId.generate());
    final var event = new CupStartedEvent(cupId, participants);

    translator.handle(event);

    assertThat(published).hasSize(1);
    final var notification = (CupEventNotification) published.getFirst();
    assertThat(notification.cupId()).isEqualTo(cupId);
    assertThat(notification.eventType()).isEqualTo("CUP_STARTED");
    assertThat(notification.recipients()).containsExactlyInAnyOrderElementsOf(participants);
  }

}
