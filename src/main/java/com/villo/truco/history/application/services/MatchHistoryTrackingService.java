package com.villo.truco.history.application.services;

import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.model.match.events.MatchAbandonedEvent;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.history.domain.model.MatchEndReason;
import com.villo.truco.history.domain.model.MatchHistoryEntry;
import com.villo.truco.history.domain.model.MatchOutcome;
import com.villo.truco.history.domain.model.PlayerMatchHistory;
import com.villo.truco.history.domain.ports.PlayerMatchHistoryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class MatchHistoryTrackingService {

  private final UserQueryRepository userQueryRepository;
  private final PlayerMatchHistoryRepository playerMatchHistoryRepository;

  public MatchHistoryTrackingService(final UserQueryRepository userQueryRepository,
      final PlayerMatchHistoryRepository playerMatchHistoryRepository) {

    this.userQueryRepository = Objects.requireNonNull(userQueryRepository);
    this.playerMatchHistoryRepository = Objects.requireNonNull(playerMatchHistoryRepository);
  }

  public void handle(final MatchDomainEvent event) {

    final var outcome = this.resolveFinalOutcome(event);
    if (outcome == null || event.getPlayerTwo() == null) {
      return;
    }

    final var registeredPlayers = this.userQueryRepository.findUsernamesByIds(
        Set.of(event.getPlayerOne(), event.getPlayerTwo())).keySet();
    final var endedAt = Instant.ofEpochMilli(event.getTimestamp());

    for (final var seat : List.of(PlayerSeat.PLAYER_ONE, PlayerSeat.PLAYER_TWO)) {
      final var playerId = event.resolvePlayer(seat);
      if (!registeredPlayers.contains(playerId)) {
        continue;
      }

      final var opponentSeat =
          seat == PlayerSeat.PLAYER_ONE ? PlayerSeat.PLAYER_TWO : PlayerSeat.PLAYER_ONE;
      final var matchOutcome = seat == outcome.winnerSeat() ? MatchOutcome.WON : MatchOutcome.LOST;
      final var ownGamesWon =
          seat == PlayerSeat.PLAYER_ONE ? outcome.gamesWonPlayerOne() : outcome.gamesWonPlayerTwo();
      final var opponentGamesWon =
          seat == PlayerSeat.PLAYER_ONE ? outcome.gamesWonPlayerTwo() : outcome.gamesWonPlayerOne();

      final var entry = new MatchHistoryEntry(event.getMatchId(), event.resolvePlayer(opponentSeat),
          matchOutcome, outcome.endReason(), ownGamesWon, opponentGamesWon, endedAt);

      final var history = this.playerMatchHistoryRepository.findByPlayerId(playerId)
          .orElseGet(() -> PlayerMatchHistory.create(playerId));
      if (history.record(entry)) {
        this.playerMatchHistoryRepository.save(history);
      }
    }
  }

  private FinalOutcome resolveFinalOutcome(final MatchDomainEvent event) {

    return switch (event) {
      case MatchFinishedEvent e ->
          new FinalOutcome(MatchEndReason.FINISHED, e.getWinnerSeat(), e.getGamesWonPlayerOne(),
              e.getGamesWonPlayerTwo());
      case MatchForfeitedEvent e ->
          new FinalOutcome(MatchEndReason.FORFEITED, e.getWinnerSeat(), e.getGamesWonPlayerOne(),
              e.getGamesWonPlayerTwo());
      case MatchAbandonedEvent e ->
          new FinalOutcome(MatchEndReason.ABANDONED, e.getWinnerSeat(), e.getGamesWonPlayerOne(),
              e.getGamesWonPlayerTwo());
      default -> null;
    };
  }

  private record FinalOutcome(MatchEndReason endReason, PlayerSeat winnerSeat,
                              int gamesWonPlayerOne, int gamesWonPlayerTwo) {

  }

}
