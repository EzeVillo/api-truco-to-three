package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.villo.truco.application.ports.out.CupDomainEventHandler;
import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutKey;
import com.villo.truco.application.ports.out.timeout.TimeoutScheduler;
import com.villo.truco.application.timeout.CupTimeoutPhasePolicy;
import com.villo.truco.domain.model.cup.events.CupCancelledEvent;
import com.villo.truco.domain.model.cup.events.CupDomainEvent;
import com.villo.truco.domain.model.cup.events.CupFinishedEvent;
import com.villo.truco.domain.model.cup.events.CupPlayerJoinedEvent;
import com.villo.truco.domain.model.cup.events.CupStartedEvent;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CupTimeoutEventHandler")
class CupTimeoutEventHandlerTest {

  private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(5);
  private final CupId cupId = CupId.generate();
  private final PlayerId playerOne = PlayerId.generate();
  private TimeoutScheduler timeoutScheduler;
  private CupDomainEventHandler<CupDomainEvent> handler;

  @BeforeEach
  void setUp() {

    timeoutScheduler = mock(TimeoutScheduler.class);
    final var dispatcher = new TimeoutActionDispatcher();
    dispatcher.register(EntityType.CUP, id -> () -> {
    });

    handler = new CupTimeoutEventHandler(timeoutScheduler, dispatcher, new CupTimeoutPhasePolicy(),
        IDLE_TIMEOUT);
  }

  @Test
  @DisplayName("Debe programar timeout al recibir evento de actividad de cup")
  void scheduleTimeoutOnActivityEvent() {

    final var event = new CupPlayerJoinedEvent(cupId, List.of(playerOne), playerOne);

    handler.handle(event);

    final var expectedKey = TimeoutKey.of(EntityType.CUP, cupId.value().toString());
    verify(timeoutScheduler).schedule(eq(expectedKey), any(Instant.class), any(Runnable.class));
  }

  @Test
  @DisplayName("Debe cancelar timeout al recibir CupCancelledEvent")
  void cancelTimeoutOnCupCancelled() {

    final var event = new CupCancelledEvent(cupId, List.of(playerOne));

    handler.handle(event);

    final var expectedKey = TimeoutKey.of(EntityType.CUP, cupId.value().toString());
    verify(timeoutScheduler).cancel(expectedKey);
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("Debe cancelar timeout al recibir CupFinishedEvent")
  void cancelTimeoutOnCupFinished() {

    final var event = new CupFinishedEvent(cupId, List.of(playerOne), playerOne);

    handler.handle(event);

    final var expectedKey = TimeoutKey.of(EntityType.CUP, cupId.value().toString());
    verify(timeoutScheduler).cancel(expectedKey);
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("Debe cancelar el timeout de torneo al arrancar (CupStartedEvent)")
  void cancelTimeoutOnCupStarted() {

    final var event = new CupStartedEvent(cupId, List.of(playerOne));

    handler.handle(event);

    final var expectedKey = TimeoutKey.of(EntityType.CUP, cupId.value().toString());
    verify(timeoutScheduler).cancel(expectedKey);
    verify(timeoutScheduler, never()).schedule(any(), any(), any());
  }

  @Test
  @DisplayName("Devuelve CupDomainEvent como tipo de evento escuchado")
  void handlerAcceptsCupDomainEventClass() {

    assertThat(handler.eventType()).isEqualTo(CupDomainEvent.class);
  }

}
