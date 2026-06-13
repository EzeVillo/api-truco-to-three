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
import com.villo.truco.application.timeout.MatchTimeoutPhasePolicy;
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
import org.mockito.ArgumentCaptor;

@DisplayName("MatchTimeoutEventHandler")
class MatchTimeoutEventHandlerTest {

  private static final Duration LOBBY_TIMEOUT = Duration.ofMinutes(5);
  private static final Duration PLAY_TIMEOUT = Duration.ofSeconds(30);
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

    handler = new MatchTimeoutEventHandler(timeoutScheduler, dispatcher,
        new MatchTimeoutPhasePolicy(), LOBBY_TIMEOUT, PLAY_TIMEOUT);
  }

  @Test
  @DisplayName("Un evento de lobby programa el timeout con la duracion de lobby")
  void scheduleLobbyTimeoutOnLobbyEvent() {

    final var before = Instant.now();
    handler.handle(new PlayerJoinedEvent(matchId, playerOne, null));

    final var deadline = captureScheduledDeadline();
    assertThat(deadline).isBetween(before.plus(LOBBY_TIMEOUT).minusSeconds(5),
        Instant.now().plus(LOBBY_TIMEOUT).plusSeconds(5));
  }

  @Test
  @DisplayName("Un evento de juego programa el timeout con la duracion de turno")
  void schedulePlayTimeoutOnGameEvent() {

    final var before = Instant.now();
    handler.handle(new GameStartedEvent(matchId, playerOne, playerTwo, 1));

    final var deadline = captureScheduledDeadline();
    assertThat(deadline).isBetween(before.plus(PLAY_TIMEOUT).minusSeconds(5),
        Instant.now().plus(PLAY_TIMEOUT).plusSeconds(5));
  }

  @Test
  @DisplayName("Debe cancelar timeout al recibir MatchFinishedEvent")
  void cancelTimeoutOnMatchFinished() {

    handler.handle(
        new MatchFinishedEvent(matchId, playerOne, playerTwo, PlayerSeat.PLAYER_ONE, 3, 1));

    verify(timeoutScheduler).cancel(expectedKey());
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("Debe cancelar timeout al recibir MatchForfeitedEvent")
  void cancelTimeoutOnMatchForfeited() {

    handler.handle(
        new MatchForfeitedEvent(matchId, playerOne, playerTwo, PlayerSeat.PLAYER_ONE, 3, 1));

    verify(timeoutScheduler).cancel(expectedKey());
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("Debe cancelar timeout al recibir MatchCancelledEvent")
  void cancelTimeoutOnMatchCancelled() {

    handler.handle(new MatchCancelledEvent(matchId, playerOne, null));

    verify(timeoutScheduler).cancel(expectedKey());
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("Debe cancelar timeout al recibir MatchAbandonedEvent")
  void cancelTimeoutOnMatchAbandoned() {

    handler.handle(new MatchAbandonedEvent(matchId, playerOne, playerTwo, PlayerSeat.PLAYER_TWO,
        PlayerSeat.PLAYER_ONE, 2, 1));

    verify(timeoutScheduler).cancel(expectedKey());
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("Debe programar timeout cuando el evento llega en sobre con inner no terminal")
  void scheduleTimeoutOnEnvelopedActivityEvent() {

    final var inner = new GameStartedEvent(matchId, playerOne, playerTwo, 1);
    final var envelope = new MatchEventEnvelope(matchId, playerOne, playerTwo, inner);

    handler.handle(envelope);

    verify(timeoutScheduler).schedule(eq(expectedKey()), any(Instant.class), any(Runnable.class));
  }

  @Test
  @DisplayName("Devuelve MatchDomainEvent como tipo de evento escuchado")
  void handlerAcceptsMatchDomainEventClass() {

    assertThat(handler.eventType()).isEqualTo(MatchDomainEvent.class);
  }

  private TimeoutKey expectedKey() {

    return TimeoutKey.of(EntityType.MATCH, matchId.value().toString());
  }

  private Instant captureScheduledDeadline() {

    final var captor = ArgumentCaptor.forClass(Instant.class);
    verify(timeoutScheduler).schedule(eq(expectedKey()), captor.capture(), any(Runnable.class));
    return captor.getValue();
  }

}
