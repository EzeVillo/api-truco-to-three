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
import com.villo.truco.domain.model.match.events.ActionDeadlineSetEvent;
import com.villo.truco.domain.model.match.events.GameStartedEvent;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.MatchEventEnvelope;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.PlayerJoinedEvent;
import com.villo.truco.domain.model.match.valueobjects.MatchStatus;
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
  @DisplayName("Un match en lobby programa el timeout con la duracion de lobby")
  void scheduleLobbyTimeoutWhenMatchInLobby() {

    final var before = Instant.now();
    handler.handle(stamped(new PlayerJoinedEvent(matchId, playerOne, null), MatchStatus.READY));

    final var deadline = captureScheduledDeadline();
    assertThat(deadline).isBetween(before.plus(LOBBY_TIMEOUT).minusSeconds(5),
        Instant.now().plus(LOBBY_TIMEOUT).plusSeconds(5));
  }

  @Test
  @DisplayName("Un match en curso programa el timeout con la duracion de turno")
  void schedulePlayTimeoutWhenMatchInProgress() {

    final var before = Instant.now();
    handler.handle(
        stamped(new GameStartedEvent(matchId, playerOne, playerTwo, 1), MatchStatus.IN_PROGRESS));

    final var deadline = captureScheduledDeadline();
    assertThat(deadline).isBetween(before.plus(PLAY_TIMEOUT).minusSeconds(5),
        Instant.now().plus(PLAY_TIMEOUT).plusSeconds(5));
  }

  @Test
  @DisplayName(
      "La fase se deriva del status, no del tipo de evento: un evento de lobby en un match "
          + "en curso usa la duracion de turno")
  void phaseFollowsStatusNotEventType() {

    final var before = Instant.now();
    handler.handle(
        stamped(new PlayerJoinedEvent(matchId, playerOne, playerTwo), MatchStatus.IN_PROGRESS));

    final var deadline = captureScheduledDeadline();
    assertThat(deadline).isBetween(before.plus(PLAY_TIMEOUT).minusSeconds(5),
        Instant.now().plus(PLAY_TIMEOUT).plusSeconds(5));
  }

  @Test
  @DisplayName("Debe cancelar timeout cuando el match esta terminado")
  void cancelTimeoutWhenMatchFinished() {

    handler.handle(
        stamped(new MatchFinishedEvent(matchId, playerOne, playerTwo, PlayerSeat.PLAYER_ONE, 3, 1),
            MatchStatus.FINISHED));

    verify(timeoutScheduler).cancel(expectedKey());
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("Debe cancelar timeout cuando el match esta cancelado")
  void cancelTimeoutWhenMatchCancelled() {

    handler.handle(
        stamped(new GameStartedEvent(matchId, playerOne, playerTwo, 1), MatchStatus.CANCELLED));

    verify(timeoutScheduler).cancel(expectedKey());
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("No programa timeout y cancela cuando el match no forfeitea por inactividad (vs bot)")
  void cancelsAndDoesNotScheduleWhenNoInactivityForfeit() {

    final var event = new GameStartedEvent(matchId, playerOne, playerTwo, 1);
    event.setMatchStatus(MatchStatus.IN_PROGRESS);
    event.setForfeitsOnInactivity(false);

    handler.handle(event);

    verify(timeoutScheduler).cancel(expectedKey());
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("Ignora los eventos de deadline derivados")
  void ignoresDerivedDeadlineEvents() {

    handler.handle(
        stamped(new ActionDeadlineSetEvent(matchId, playerOne, playerTwo, PlayerSeat.PLAYER_ONE),
            MatchStatus.IN_PROGRESS));

    verify(timeoutScheduler, never()).schedule(any(), any(), any());
    verify(timeoutScheduler, never()).cancel(any());
  }

  @Test
  @DisplayName("Debe programar timeout cuando el evento llega en sobre")
  void scheduleTimeoutOnEnvelopedEvent() {

    final var inner = new GameStartedEvent(matchId, playerOne, playerTwo, 1);
    final var envelope = stamped(new MatchEventEnvelope(matchId, playerOne, playerTwo, inner),
        MatchStatus.IN_PROGRESS);

    handler.handle(envelope);

    verify(timeoutScheduler).schedule(eq(expectedKey()), any(Instant.class), any(Runnable.class));
  }

  @Test
  @DisplayName("Devuelve MatchDomainEvent como tipo de evento escuchado")
  void handlerAcceptsMatchDomainEventClass() {

    assertThat(handler.eventType()).isEqualTo(MatchDomainEvent.class);
  }

  private MatchDomainEvent stamped(final MatchDomainEvent event, final MatchStatus status) {

    event.setMatchStatus(status);
    return event;
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
