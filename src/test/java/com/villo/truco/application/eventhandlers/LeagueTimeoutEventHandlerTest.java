package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.villo.truco.application.ports.out.LeagueDomainEventHandler;
import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutKey;
import com.villo.truco.application.ports.out.timeout.TimeoutScheduler;
import com.villo.truco.domain.model.league.events.LeagueCancelledEvent;
import com.villo.truco.domain.model.league.events.LeagueDomainEvent;
import com.villo.truco.domain.model.league.events.LeagueFinishedEvent;
import com.villo.truco.domain.model.league.events.LeaguePlayerJoinedEvent;
import com.villo.truco.domain.model.league.events.LeagueStartedEvent;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LeagueTimeoutEventHandler")
class LeagueTimeoutEventHandlerTest {

  private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(5);
  private final LeagueId leagueId = LeagueId.generate();
  private final PlayerId playerOne = PlayerId.generate();
  private TimeoutScheduler timeoutScheduler;
  private LeagueDomainEventHandler<LeagueDomainEvent> handler;

  @BeforeEach
  void setUp() {

    timeoutScheduler = mock(TimeoutScheduler.class);
    final var dispatcher = new TimeoutActionDispatcher();
    dispatcher.register(EntityType.LEAGUE, id -> () -> {
    });

    handler = new LeagueTimeoutEventHandler(timeoutScheduler, dispatcher, IDLE_TIMEOUT);
  }

  @Test
  @DisplayName("Debe programar timeout al recibir evento de actividad de league")
  void scheduleTimeoutOnActivityEvent() {

    final var event = new LeaguePlayerJoinedEvent(leagueId, List.of(playerOne), playerOne);

    handler.handle(event);

    final var expectedKey = TimeoutKey.of(EntityType.LEAGUE, leagueId.value().toString());
    verify(timeoutScheduler).schedule(eq(expectedKey), any(Instant.class), any(Runnable.class));
  }

  @Test
  @DisplayName("Debe cancelar timeout al recibir LeagueCancelledEvent")
  void cancelTimeoutOnLeagueCancelled() {

    final var event = new LeagueCancelledEvent(leagueId, List.of(playerOne));

    handler.handle(event);

    final var expectedKey = TimeoutKey.of(EntityType.LEAGUE, leagueId.value().toString());
    verify(timeoutScheduler).cancel(expectedKey);
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("Debe cancelar timeout al recibir LeagueFinishedEvent")
  void cancelTimeoutOnLeagueFinished() {

    final var event = new LeagueFinishedEvent(leagueId, List.of(playerOne), List.of(playerOne));

    handler.handle(event);

    final var expectedKey = TimeoutKey.of(EntityType.LEAGUE, leagueId.value().toString());
    verify(timeoutScheduler).cancel(expectedKey);
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("Debe programar timeout ante evento no terminal (LeagueStartedEvent)")
  void scheduleTimeoutOnLeagueStarted() {

    final var event = new LeagueStartedEvent(leagueId, List.of(playerOne));

    handler.handle(event);

    final var expectedKey = TimeoutKey.of(EntityType.LEAGUE, leagueId.value().toString());
    verify(timeoutScheduler).schedule(eq(expectedKey), any(Instant.class), any(Runnable.class));
  }

  @Test
  @DisplayName("Devuelve LeagueDomainEvent como tipo de evento escuchado")
  void handlerAcceptsLeagueDomainEventClass() {

    assertThat(handler.eventType()).isEqualTo(LeagueDomainEvent.class);
  }

}
