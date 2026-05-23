package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.application.commands.CancelQuickMatchSearchCommand;
import com.villo.truco.domain.model.quickmatch.QuickMatchTicket;
import com.villo.truco.domain.ports.QuickMatchQueuePort;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CancelQuickMatchSearchCommandHandler")
class CancelQuickMatchSearchCommandHandlerTest {

  private QuickMatchQueuePort queuePort;
  private CancelQuickMatchSearchCommandHandler handler;

  @BeforeEach
  void setUp() {

    queuePort = mock(QuickMatchQueuePort.class);
    handler = new CancelQuickMatchSearchCommandHandler(queuePort);
  }

  @Test
  @DisplayName("player in queue: tryDequeue called once")
  void playerInQueue() {

    final var player = PlayerId.generate();
    final var ticket = new QuickMatchTicket(player, GamesToPlay.of(3), Instant.now(), null);
    when(queuePort.tryDequeue(player)).thenReturn(Optional.of(ticket));

    handler.handle(new CancelQuickMatchSearchCommand(player));

    verify(queuePort).tryDequeue(player);
  }

  @Test
  @DisplayName("player not in queue: no-op, no exception thrown")
  void playerNotInQueue() {

    final var player = PlayerId.generate();
    when(queuePort.tryDequeue(any())).thenReturn(Optional.empty());

    assertThatCode(
        () -> handler.handle(new CancelQuickMatchSearchCommand(player))).doesNotThrowAnyException();
    verify(queuePort).tryDequeue(player);
  }

}
