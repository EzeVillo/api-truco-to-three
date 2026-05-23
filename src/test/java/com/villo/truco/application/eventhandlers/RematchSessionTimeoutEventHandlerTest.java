package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.villo.truco.application.ports.out.RematchSessionDomainEventHandler;
import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutKey;
import com.villo.truco.application.ports.out.timeout.TimeoutScheduler;
import com.villo.truco.domain.model.rematch.events.RematchSessionClosedByLeaveEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionConfirmedEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionDomainEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionExpiredEvent;
import com.villo.truco.domain.model.rematch.events.RematchSessionOpenedEvent;
import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionId;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RematchSessionTimeoutEventHandler")
class RematchSessionTimeoutEventHandlerTest {

  private final RematchSessionId sessionId = RematchSessionId.generate();
  private final MatchId originMatchId = MatchId.generate();
  private final PlayerId playerOne = PlayerId.generate();
  private final PlayerId playerTwo = PlayerId.generate();
  private TimeoutScheduler timeoutScheduler;
  private RematchSessionDomainEventHandler<RematchSessionDomainEvent> handler;

  @BeforeEach
  void setUp() {

    timeoutScheduler = mock(TimeoutScheduler.class);
    final var dispatcher = new TimeoutActionDispatcher();
    dispatcher.register(EntityType.REMATCH_SESSION, id -> () -> {
    });

    handler = new RematchSessionTimeoutEventHandler(timeoutScheduler, dispatcher);
  }

  @Test
  @DisplayName("Debe programar timeout al crear sesión con expiresAt del evento")
  void scheduleTimeoutOnSessionOpened() {

    final var expiresAt = Instant.now().plusSeconds(120);
    final var event = new RematchSessionOpenedEvent(sessionId, originMatchId, playerOne, playerTwo,
        expiresAt, false, false);

    handler.handle(event);

    final var expectedKey = TimeoutKey.of(EntityType.REMATCH_SESSION, sessionId.value().toString());
    verify(timeoutScheduler).schedule(eq(expectedKey), eq(expiresAt), any(Runnable.class));
  }

  @Test
  @DisplayName("Debe cancelar timeout cuando ambos jugadores confirman rematch")
  void cancelTimeoutOnSessionConfirmed() {

    final var event = new RematchSessionConfirmedEvent(sessionId, originMatchId, MatchId.generate(),
        playerOne, playerTwo, 3);

    handler.handle(event);

    final var expectedKey = TimeoutKey.of(EntityType.REMATCH_SESSION, sessionId.value().toString());
    verify(timeoutScheduler).cancel(expectedKey);
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("Debe cancelar timeout cuando un jugador abandona la sesión")
  void cancelTimeoutOnSessionClosedByLeave() {

    final var event = new RematchSessionClosedByLeaveEvent(sessionId, originMatchId, playerOne,
        playerTwo);

    handler.handle(event);

    final var expectedKey = TimeoutKey.of(EntityType.REMATCH_SESSION, sessionId.value().toString());
    verify(timeoutScheduler).cancel(expectedKey);
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("Debe cancelar timeout cuando la sesión expira")
  void cancelTimeoutOnSessionExpired() {

    final var event = new RematchSessionExpiredEvent(sessionId, originMatchId, playerOne,
        playerTwo);

    handler.handle(event);

    final var expectedKey = TimeoutKey.of(EntityType.REMATCH_SESSION, sessionId.value().toString());
    verify(timeoutScheduler).cancel(expectedKey);
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("Devuelve RematchSessionDomainEvent como tipo de evento escuchado")
  void handlerAcceptsRematchSessionDomainEventClass() {

    assertThat(handler.eventType()).isEqualTo(RematchSessionDomainEvent.class);
  }

}
