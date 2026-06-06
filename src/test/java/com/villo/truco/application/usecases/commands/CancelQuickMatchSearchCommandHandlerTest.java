package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.application.commands.CancelQuickMatchSearchCommand;
import com.villo.truco.application.eventhandlers.PresenceNotifier;
import com.villo.truco.domain.model.quickmatch.QuickMatchTicket;
import com.villo.truco.domain.ports.QuickMatchQueuePort;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.services.FriendAvailabilityChangeNotifier;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CancelQuickMatchSearchCommandHandler")
class CancelQuickMatchSearchCommandHandlerTest {

  private QuickMatchQueuePort queuePort;
  private FriendAvailabilityChangeNotifier friendAvailabilityChangeNotifier;
  private PresenceNotifier presenceNotifier;
  private CancelQuickMatchSearchCommandHandler handler;

  @BeforeEach
  void setUp() {

    queuePort = mock(QuickMatchQueuePort.class);
    friendAvailabilityChangeNotifier = mock(FriendAvailabilityChangeNotifier.class);
    presenceNotifier = mock(PresenceNotifier.class);
    handler = new CancelQuickMatchSearchCommandHandler(queuePort, friendAvailabilityChangeNotifier,
        presenceNotifier);
    lenient().when(queuePort.findByPlayer(any())).thenReturn(Optional.empty());
  }

  @Test
  @DisplayName("player in queue: tryDequeue called once")
  void playerInQueue() {

    final var player = PlayerId.generate();
    final var ticket = new QuickMatchTicket(player, GamesToPlay.of(3), Instant.now(), null);
    when(queuePort.tryDequeue(player)).thenReturn(Optional.of(ticket));

    handler.handle(new CancelQuickMatchSearchCommand(player.toString()));

    verify(queuePort).tryDequeue(player);
    verify(friendAvailabilityChangeNotifier).notifyAvailabilityChanged(eq(player), anyLong());
    verify(presenceNotifier).notifyPlayers(List.of(player));
  }

  @Test
  @DisplayName("player not in queue: no-op, no exception thrown")
  void playerNotInQueue() {

    final var player = PlayerId.generate();
    when(queuePort.tryDequeue(any())).thenReturn(Optional.empty());

    assertThatCode(() -> handler.handle(
        new CancelQuickMatchSearchCommand(player.toString()))).doesNotThrowAnyException();
    verify(queuePort).tryDequeue(player);
    verify(friendAvailabilityChangeNotifier, never()).notifyAvailabilityChanged(any(), anyLong());
    verify(presenceNotifier, never()).notifyPlayers(any());
  }

  @Test
  @DisplayName("sesion asociada a la cola: cancela por sessionId y notifica al duenio del ticket")
  void sessionInQueue() {

    final var player = PlayerId.generate();
    final var ticket = new QuickMatchTicket(player, GamesToPlay.of(3), Instant.now(), "ws-1");
    when(queuePort.tryDequeueBySessionId("ws-1")).thenReturn(Optional.of(ticket));

    handler.handle(new CancelQuickMatchSearchCommand(player, "ws-1"));

    verify(queuePort).tryDequeueBySessionId("ws-1");
    verify(queuePort, never()).tryDequeue(player);
    verify(friendAvailabilityChangeNotifier).notifyAvailabilityChanged(eq(player), anyLong());
    verify(presenceNotifier).notifyPlayers(List.of(player));
  }

  @Test
  @DisplayName("desconexion de otra sesion no cancela ticket asociado a una sesion distinta")
  void differentSessionDoesNotCancelAssociatedTicket() {

    final var player = PlayerId.generate();
    final var ticket = new QuickMatchTicket(player, GamesToPlay.of(3), Instant.now(), "ws-1");
    when(queuePort.tryDequeueBySessionId("ws-2")).thenReturn(Optional.empty());
    when(queuePort.findByPlayer(player)).thenReturn(Optional.of(ticket));

    handler.handle(new CancelQuickMatchSearchCommand(player, "ws-2"));

    verify(queuePort, never()).tryDequeue(player);
    verify(friendAvailabilityChangeNotifier, never()).notifyAvailabilityChanged(any(), anyLong());
    verify(presenceNotifier, never()).notifyPlayers(any());
  }

  @Test
  @DisplayName("ticket sin sessionId conserva fallback por jugador al desconectar")
  void legacyTicketWithoutSessionFallsBackToPlayer() {

    final var player = PlayerId.generate();
    final var ticket = new QuickMatchTicket(player, GamesToPlay.of(3), Instant.now(), null);
    when(queuePort.tryDequeueBySessionId("ws-1")).thenReturn(Optional.empty());
    when(queuePort.findByPlayer(player)).thenReturn(Optional.of(ticket));
    when(queuePort.tryDequeue(player)).thenReturn(Optional.of(ticket));

    handler.handle(new CancelQuickMatchSearchCommand(player, "ws-1"));

    verify(queuePort).tryDequeue(player);
    verify(friendAvailabilityChangeNotifier).notifyAvailabilityChanged(eq(player), anyLong());
    verify(presenceNotifier).notifyPlayers(List.of(player));
  }

}
