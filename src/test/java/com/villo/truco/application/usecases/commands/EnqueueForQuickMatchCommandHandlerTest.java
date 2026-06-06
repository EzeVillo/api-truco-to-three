package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.application.commands.EnqueueForQuickMatchCommand;
import com.villo.truco.application.dto.QuickMatchStatus;
import com.villo.truco.application.eventhandlers.PresenceNotifier;
import com.villo.truco.domain.model.match.exceptions.PlayerAlreadyInActiveMatchException;
import com.villo.truco.domain.model.quickmatch.QuickMatchTicket;
import com.villo.truco.domain.ports.MatchEventNotifier;
import com.villo.truco.domain.ports.MatchRepository;
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

@DisplayName("EnqueueForQuickMatchCommandHandler")
class EnqueueForQuickMatchCommandHandlerTest {

  private static final GamesToPlay THREE = GamesToPlay.of(3);
  private QuickMatchQueuePort queuePort;
  private PlayerAvailabilityChecker availabilityChecker;
  private MatchRepository matchRepository;
  private MatchEventNotifier matchEventNotifier;
  private FriendAvailabilityChangeNotifier friendAvailabilityChangeNotifier;
  private PresenceNotifier presenceNotifier;
  private EnqueueForQuickMatchCommandHandler handler;

  @BeforeEach
  void setUp() {

    queuePort = mock(QuickMatchQueuePort.class);
    availabilityChecker = mock(PlayerAvailabilityChecker.class);
    matchRepository = mock(MatchRepository.class);
    matchEventNotifier = mock(MatchEventNotifier.class);
    friendAvailabilityChangeNotifier = mock(FriendAvailabilityChangeNotifier.class);
    presenceNotifier = mock(PresenceNotifier.class);
    handler = new EnqueueForQuickMatchCommandHandler(queuePort, availabilityChecker,
        matchRepository, matchEventNotifier, friendAvailabilityChangeNotifier, presenceNotifier);
  }

  private EnqueueForQuickMatchCommand command(final PlayerId playerId) {

    return new EnqueueForQuickMatchCommand(playerId, THREE, null);
  }

  @Test
  @DisplayName("pairing path: opponent in queue → MATCHED, match created, events published")
  void pairingPath() {

    final var player = PlayerId.generate();
    final var opponent = PlayerId.generate();
    final var opponentTicket = new QuickMatchTicket(opponent, THREE, Instant.now(), null);

    when(queuePort.findByPlayer(player)).thenReturn(Optional.empty());
    when(queuePort.tryMatchOpponent(player, THREE)).thenReturn(Optional.of(opponentTicket));

    final var result = handler.handle(command(player));

    assertThat(result.status()).isEqualTo(QuickMatchStatus.MATCHED);
    assertThat(result.matchId()).isNotNull();
    verify(matchRepository).save(any());
    verify(matchEventNotifier).publishDomainEvents(any());
    verify(friendAvailabilityChangeNotifier, never()).notifyAvailabilityChanged(any(), anyLong());
    verify(presenceNotifier, never()).notifyPlayers(any());
    verify(queuePort, never()).enqueue(any());
  }

  @Test
  @DisplayName("no-opponent path: queue empty → SEARCHING, ticket enqueued")
  void noOpponentPath() {

    final var player = PlayerId.generate();

    when(queuePort.findByPlayer(player)).thenReturn(Optional.empty());
    when(queuePort.tryMatchOpponent(player, THREE)).thenReturn(Optional.empty());

    final var result = handler.handle(command(player));

    assertThat(result.status()).isEqualTo(QuickMatchStatus.SEARCHING);
    assertThat(result.matchId()).isNull();
    verify(queuePort).enqueue(any());
    verify(friendAvailabilityChangeNotifier).notifyAvailabilityChanged(eq(player), anyLong());
    verify(presenceNotifier).notifyPlayers(List.of(player));
    verify(matchRepository, never()).save(any());
  }

  @Test
  @DisplayName("idempotency: player already in queue → SEARCHING with original enqueuedAt")
  void idempotencyReturnsExistingTicket() {

    final var player = PlayerId.generate();
    final var originalTime = Instant.parse("2026-05-20T10:00:00Z");
    final var existing = new QuickMatchTicket(player, THREE, originalTime, null);

    when(queuePort.findByPlayer(player)).thenReturn(Optional.of(existing));

    final var result = handler.handle(command(player));

    assertThat(result.status()).isEqualTo(QuickMatchStatus.SEARCHING);
    assertThat(result.enqueuedAt()).isEqualTo(originalTime);
    verify(queuePort, never()).enqueue(any());
    verify(friendAvailabilityChangeNotifier, never()).notifyAvailabilityChanged(any(), anyLong());
    verify(presenceNotifier, never()).notifyPlayers(any());
    verify(availabilityChecker, never()).ensureAvailable(any());
  }

  @Test
  @DisplayName("unavailable player: ensureAvailable throws → exception propagates")
  void unavailablePlayer() {

    final var player = PlayerId.generate();

    when(queuePort.findByPlayer(player)).thenReturn(Optional.empty());
    doThrow(new PlayerAlreadyInActiveMatchException()).when(availabilityChecker)
        .ensureAvailable(player);

    assertThatThrownBy(() -> handler.handle(command(player))).isInstanceOf(
        PlayerAlreadyInActiveMatchException.class);

    verify(matchRepository, never()).save(any());
    verify(friendAvailabilityChangeNotifier, never()).notifyAvailabilityChanged(any(), anyLong());
    verify(presenceNotifier, never()).notifyPlayers(any());
  }

  @Test
  @DisplayName("no intermediate events: matchEventNotifier not called between enqueue and pairing (FR-012)")
  void noIntermediateNotifications() {

    final var player = PlayerId.generate();

    when(queuePort.findByPlayer(player)).thenReturn(Optional.empty());
    when(queuePort.tryMatchOpponent(player, THREE)).thenReturn(Optional.empty());

    handler.handle(command(player));

    verify(matchEventNotifier, never()).publishDomainEvents(any());
  }

}
