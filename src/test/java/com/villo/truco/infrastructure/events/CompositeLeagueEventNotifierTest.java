package com.villo.truco.infrastructure.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.villo.truco.application.ports.out.LeagueDomainEventHandler;
import com.villo.truco.application.ports.out.LeagueEventContext;
import com.villo.truco.domain.model.league.events.LeagueCancelledEvent;
import com.villo.truco.domain.model.league.events.LeagueStartedEvent;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompositeLeagueEventNotifier")
class CompositeLeagueEventNotifierTest {

    private LeagueId leagueId;
    private List<PlayerId> participants;

    @BeforeEach
    void setUp() {

        this.leagueId = LeagueId.generate();
        this.participants = List.of(PlayerId.generate(), PlayerId.generate());
    }

    private <E extends DomainEventBase> LeagueDomainEventHandler<E> handlerFor(final Class<E> type,
        final List<DomainEventBase> received) {

        return new LeagueDomainEventHandler<>() {

            @Override
            public Class<E> eventType() {

                return type;
            }

            @Override
            public void handle(final E event, final LeagueEventContext ctx) {

                received.add(event);
            }
        };
    }

    @Test
    @DisplayName("rutea LeagueStartedEvent al handler de LeagueStartedEvent")
    void routesEventToMatchingHandler() {

        final var received = new ArrayList<DomainEventBase>();
        final var notifier = new CompositeLeagueEventNotifier(
            List.of(handlerFor(LeagueStartedEvent.class, received)));

        final var event = new LeagueStartedEvent(this.leagueId);
        notifier.publishDomainEvents(this.leagueId, this.participants, List.of(event));

        assertThat(received).containsExactly(event);
    }

    @Test
    @DisplayName("handler wildcard (DomainEventBase) recibe todos los eventos")
    void wildcardHandlerReceivesAllEvents() {

        final var received = new ArrayList<DomainEventBase>();
        final var notifier = new CompositeLeagueEventNotifier(
            List.of(handlerFor(DomainEventBase.class, received)));

        final var started = new LeagueStartedEvent(this.leagueId);
        final var cancelled = new LeagueCancelledEvent(this.leagueId);
        notifier.publishDomainEvents(this.leagueId, this.participants, List.of(started, cancelled));

        assertThat(received).containsExactly(started, cancelled);
    }

    @Test
    @DisplayName("evento sin handler registrado no lanza excepción")
    void noHandlerForEventDoesNotThrow() {

        final var notifier = new CompositeLeagueEventNotifier(
            List.of(handlerFor(LeagueCancelledEvent.class, new ArrayList<>())));

        assertThatNoException().isThrownBy(
            () -> notifier.publishDomainEvents(this.leagueId, this.participants,
                List.of(new LeagueStartedEvent(this.leagueId))));
    }

    @Test
    @DisplayName("handlers se invocan en orden de registro")
    void handlersInvokedInRegistrationOrder() {

        final var order = new ArrayList<String>();

        final LeagueDomainEventHandler<LeagueStartedEvent> first = new LeagueDomainEventHandler<>() {

            @Override
            public Class<LeagueStartedEvent> eventType() {

                return LeagueStartedEvent.class;
            }

            @Override
            public void handle(final LeagueStartedEvent event, final LeagueEventContext ctx) {

                order.add("first");
            }
        };

        final LeagueDomainEventHandler<LeagueStartedEvent> second = new LeagueDomainEventHandler<>() {

            @Override
            public Class<LeagueStartedEvent> eventType() {

                return LeagueStartedEvent.class;
            }

            @Override
            public void handle(final LeagueStartedEvent event, final LeagueEventContext ctx) {

                order.add("second");
            }
        };

        final var notifier = new CompositeLeagueEventNotifier(List.of(first, second));
        notifier.publishDomainEvents(this.leagueId, this.participants,
            List.of(new LeagueStartedEvent(this.leagueId)));

        assertThat(order).containsExactly("first", "second");
    }

}
