package com.villo.truco.infrastructure.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MatchDomainEventDispatcher")
class CompositeMatchEventNotifierTest {

  private MatchId matchId;
  private PlayerId playerOne;
  private PlayerId playerTwo;

  @BeforeEach
  void setUp() {

    this.matchId = MatchId.generate();
    this.playerOne = PlayerId.generate();
    this.playerTwo = PlayerId.generate();
  }

  private <E extends DomainEventBase> MatchDomainEventHandler<E> handlerFor(final Class<E> type,
      final List<DomainEventBase> received) {

    return new MatchDomainEventHandler<>() {

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
  @DisplayName("rutea MatchFinishedEvent al handler de MatchFinishedEvent")
  void routesEventToMatchingHandler() {

    final var received = new ArrayList<DomainEventBase>();
    final var dispatcher = new MatchDomainEventDispatcher(
        List.of(handlerFor(MatchFinishedEvent.class, received)));

    final var event = new MatchFinishedEvent(this.matchId, this.playerOne, this.playerTwo,
        PlayerSeat.PLAYER_ONE, 2, 0);
    dispatcher.publishDomainEvents(List.of(event));

    assertThat(received).containsExactly(event);
  }

  @Test
  @DisplayName("handler wildcard (DomainEventBase) recibe todos los eventos")
  void wildcardHandlerReceivesAllEvents() {

    final var received = new ArrayList<DomainEventBase>();
    final var dispatcher = new MatchDomainEventDispatcher(
        List.of(handlerFor(DomainEventBase.class, received)));

    final var finished = new MatchFinishedEvent(this.matchId, this.playerOne, this.playerTwo,
        PlayerSeat.PLAYER_ONE, 2, 0);
    final var forfeited = new MatchForfeitedEvent(this.matchId, this.playerOne, this.playerTwo,
        PlayerSeat.PLAYER_TWO, 1, 0);
    dispatcher.publishDomainEvents(List.of(finished, forfeited));

    assertThat(received).containsExactly(finished, forfeited);
  }

  @Test
  @DisplayName("evento sin handler registrado no lanza excepción")
  void noHandlerForEventDoesNotThrow() {

    final var dispatcher = new MatchDomainEventDispatcher(
        List.of(handlerFor(MatchForfeitedEvent.class, new ArrayList<>())));

    final var event = new MatchFinishedEvent(this.matchId, this.playerOne, this.playerTwo,
        PlayerSeat.PLAYER_ONE, 2, 0);
    assertThatNoException().isThrownBy(() -> dispatcher.publishDomainEvents(List.of(event)));
  }

  @Test
  @DisplayName("handlers se invocan en orden de registro")
  void handlersInvokedInRegistrationOrder() {

    final var order = new ArrayList<String>();

    final MatchDomainEventHandler<MatchFinishedEvent> first = new MatchDomainEventHandler<>() {

      @Override
      public Class<MatchFinishedEvent> eventType() {

        return MatchFinishedEvent.class;
      }

      @Override
      public void handle(final MatchFinishedEvent event) {

        order.add("first");
      }
    };

    final MatchDomainEventHandler<MatchFinishedEvent> second = new MatchDomainEventHandler<>() {

      @Override
      public Class<MatchFinishedEvent> eventType() {

        return MatchFinishedEvent.class;
      }

      @Override
      public void handle(final MatchFinishedEvent event) {

        order.add("second");
      }
    };

    final var dispatcher = new MatchDomainEventDispatcher(List.of(first, second));
    dispatcher.publishDomainEvents(List.of(
        new MatchFinishedEvent(this.matchId, this.playerOne, this.playerTwo, PlayerSeat.PLAYER_ONE,
            2, 0)));

    assertThat(order).containsExactly("first", "second");
  }

  @Test
  @DisplayName("lista vacía de eventos no invoca handlers")
  void emptyEventListDoesNotInvokeHandlers() {

    final var received = new ArrayList<DomainEventBase>();
    final var dispatcher = new MatchDomainEventDispatcher(
        List.of(handlerFor(DomainEventBase.class, received)));

    dispatcher.publishDomainEvents(List.of());

    assertThat(received).isEmpty();
  }

}
