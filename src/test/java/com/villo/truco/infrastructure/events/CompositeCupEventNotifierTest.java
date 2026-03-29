package com.villo.truco.infrastructure.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.villo.truco.application.ports.out.CupDomainEventHandler;
import com.villo.truco.domain.model.cup.events.CupCancelledEvent;
import com.villo.truco.domain.model.cup.events.CupStartedEvent;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompositeCupEventNotifier")
class CompositeCupEventNotifierTest {

  private CupId cupId;
  private List<PlayerId> participants;

  @BeforeEach
  void setUp() {

    this.cupId = CupId.generate();
    this.participants = List.of(PlayerId.generate(), PlayerId.generate());
  }

  private <E extends DomainEventBase> CupDomainEventHandler<E> handlerFor(final Class<E> type,
      final List<DomainEventBase> received) {

    return new CupDomainEventHandler<>() {

      @Override
      public Class<E> eventType() {

        return type;
      }

      @Override
      public void handle(final E event) {

        received.add(event);
      }
    };
  }

  @Test
  @DisplayName("rutea CupStartedEvent al handler de CupStartedEvent")
  void routesEventToMatchingHandler() {

    final var received = new ArrayList<DomainEventBase>();
    final var notifier = new CompositeCupEventNotifier(
        List.of(handlerFor(CupStartedEvent.class, received)));

    final var event = new CupStartedEvent(this.cupId, this.participants);
    notifier.publishDomainEvents(List.of(event));

    assertThat(received).containsExactly(event);
  }

  @Test
  @DisplayName("handler wildcard (DomainEventBase) recibe todos los eventos")
  void wildcardHandlerReceivesAllEvents() {

    final var received = new ArrayList<DomainEventBase>();
    final var notifier = new CompositeCupEventNotifier(
        List.of(handlerFor(DomainEventBase.class, received)));

    final var started = new CupStartedEvent(this.cupId, this.participants);
    final var cancelled = new CupCancelledEvent(this.cupId, this.participants);
    notifier.publishDomainEvents(List.of(started, cancelled));

    assertThat(received).containsExactly(started, cancelled);
  }

  @Test
  @DisplayName("evento sin handler registrado no lanza excepción")
  void noHandlerForEventDoesNotThrow() {

    final var notifier = new CompositeCupEventNotifier(
        List.of(handlerFor(CupCancelledEvent.class, new ArrayList<>())));

    assertThatNoException().isThrownBy(() -> notifier.publishDomainEvents(
        List.of(new CupStartedEvent(this.cupId, this.participants))));
  }

  @Test
  @DisplayName("handlers se invocan en orden de registro")
  void handlersInvokedInRegistrationOrder() {

    final var order = new ArrayList<String>();

    final CupDomainEventHandler<CupStartedEvent> first = new CupDomainEventHandler<>() {

      @Override
      public Class<CupStartedEvent> eventType() {

        return CupStartedEvent.class;
      }

      @Override
      public void handle(final CupStartedEvent event) {

        order.add("first");
      }
    };

    final CupDomainEventHandler<CupStartedEvent> second = new CupDomainEventHandler<>() {

      @Override
      public Class<CupStartedEvent> eventType() {

        return CupStartedEvent.class;
      }

      @Override
      public void handle(final CupStartedEvent event) {

        order.add("second");
      }
    };

    final var notifier = new CompositeCupEventNotifier(List.of(first, second));
    notifier.publishDomainEvents(List.of(new CupStartedEvent(cupId, participants)));

    assertThat(order).containsExactly("first", "second");
  }

}
