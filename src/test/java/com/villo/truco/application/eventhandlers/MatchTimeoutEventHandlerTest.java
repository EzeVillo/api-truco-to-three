package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.villo.truco.application.ports.out.MatchDomainEventHandler;
import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutKey;
import com.villo.truco.application.ports.out.timeout.TimeoutScheduler;
import com.villo.truco.domain.model.match.events.GameStartedEvent;
import com.villo.truco.domain.model.match.events.MatchAbandonedEvent;
import com.villo.truco.domain.model.match.events.MatchCancelledEvent;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.MatchEventEnvelope;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MatchTimeoutEventHandler")
class MatchTimeoutEventHandlerTest {

  private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(5);
  private final MatchId matchId = MatchId.generate();
  private final PlayerId playerOne = PlayerId.generate();
  private final PlayerId playerTwo = PlayerId.generate();
  private TimeoutScheduler timeoutScheduler;
  private MatchDomainEventHandler<MatchDomainEvent> handler;

  @BeforeEach
  void setUp() {

    timeoutScheduler = mock(TimeoutScheduler.class);
    final var dispatcher = new TimeoutActionDispatcher();
    dispatcher.register(EntityType.MATCH, id -> () -> {
    });

    handler = new MatchTimeoutEventHandler(timeoutScheduler, dispatcher, IDLE_TIMEOUT);
  }

  @Test
  @DisplayName("Debe programar timeout al recibir evento de actividad")
  void scheduleTimeoutOnActivityEvent() {

    final var event = new PlayerJoinedEvent(matchId, playerOne, null);

    handler.handle(event);

    final var expectedKey = TimeoutKey.of(EntityType.MATCH, matchId.value().toString());
    verify(timeoutScheduler).schedule(eq(expectedKey), any(Instant.class), any(Runnable.class));
  }

  @Test
  @DisplayName("Debe cancelar timeout al recibir MatchFinishedEvent")
  void cancelTimeoutOnMatchFinished() {

    final var event = new MatchFinishedEvent(matchId, playerOne, playerTwo, PlayerSeat.PLAYER_ONE,
        3, 1);

    handler.handle(event);

    final var expectedKey = TimeoutKey.of(EntityType.MATCH, matchId.value().toString());
    verify(timeoutScheduler).cancel(expectedKey);
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("Debe cancelar timeout al recibir MatchForfeitedEvent")
  void cancelTimeoutOnMatchForfeited() {

    final var event = new MatchForfeitedEvent(matchId, playerOne, playerTwo, PlayerSeat.PLAYER_ONE,
        3, 1);

    handler.handle(event);

    final var expectedKey = TimeoutKey.of(EntityType.MATCH, matchId.value().toString());
    verify(timeoutScheduler).cancel(expectedKey);
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("Debe cancelar timeout al recibir MatchCancelledEvent")
  void cancelTimeoutOnMatchCancelled() {

    final var event = new MatchCancelledEvent(matchId, playerOne, null);

    handler.handle(event);

    final var expectedKey = TimeoutKey.of(EntityType.MATCH, matchId.value().toString());
    verify(timeoutScheduler).cancel(expectedKey);
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("Debe cancelar timeout al recibir MatchAbandonedEvent")
  void cancelTimeoutOnMatchAbandoned() {

    final var event = new MatchAbandonedEvent(matchId, playerOne, playerTwo, PlayerSeat.PLAYER_TWO,
        PlayerSeat.PLAYER_ONE, 2, 1);

    handler.handle(event);

    final var expectedKey = TimeoutKey.of(EntityType.MATCH, matchId.value().toString());
    verify(timeoutScheduler).cancel(expectedKey);
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("Debe programar timeout cuando el evento llega en sobre (MatchEventEnvelope) con inner no terminal")
  void scheduleTimeoutOnEnvelopedActivityEvent() {

    final var inner = new GameStartedEvent(matchId, playerOne, playerTwo, 1);
    final var envelope = new MatchEventEnvelope(matchId, playerOne, playerTwo, inner);

    handler.handle(envelope);

    final var expectedKey = TimeoutKey.of(EntityType.MATCH, matchId.value().toString());
    verify(timeoutScheduler).schedule(eq(expectedKey), any(Instant.class), any(Runnable.class));
  }

  @Test
  @DisplayName("Devuelve MatchDomainEvent como tipo de evento escuchado")
  void handlerAcceptsMatchDomainEventClass() {

    assertThat(handler.eventType()).isEqualTo(MatchDomainEvent.class);
  }

}
