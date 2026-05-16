package com.villo.truco.profile.application.services;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.model.match.events.MatchAbandonedEvent;
import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import com.villo.truco.domain.model.match.events.MatchFinishedEvent;
import com.villo.truco.domain.model.match.events.MatchForfeitedEvent;
import com.villo.truco.domain.model.match.valueobjects.PlayerSeat;
import com.villo.truco.profile.domain.model.MatchOutcome;
import com.villo.truco.profile.domain.ports.PlayerStatsRepository;
import com.villo.truco.profile.domain.ports.ProcessedMatchStatsRegistry;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ProfilePlayerStatsTrackingService {

  private final BotRegistry botRegistry;
  private final UserQueryRepository userQueryRepository;
  private final PlayerStatsRepository playerStatsRepository;
  private final ProcessedMatchStatsRegistry processedMatchStatsRegistry;

  public ProfilePlayerStatsTrackingService(final BotRegistry botRegistry,
      final UserQueryRepository userQueryRepository,
      final PlayerStatsRepository playerStatsRepository,
      final ProcessedMatchStatsRegistry processedMatchStatsRegistry) {

    this.botRegistry = Objects.requireNonNull(botRegistry);
    this.userQueryRepository = Objects.requireNonNull(userQueryRepository);
    this.playerStatsRepository = Objects.requireNonNull(playerStatsRepository);
    this.processedMatchStatsRegistry = Objects.requireNonNull(processedMatchStatsRegistry);
  }

  public void handle(final MatchDomainEvent event) {

    if (event.getPlayerTwo() == null) {
      return;
    }

    final PlayerSeat winnerSeat = resolveWinnerSeat(event);
    if (winnerSeat == null) {
      return;
    }

    if (this.botRegistry.isBot(event.getPlayerOne()) || this.botRegistry.isBot(
        event.getPlayerTwo())) {
      return;
    }

    final var registeredPlayers = this.userQueryRepository.findUsernamesByIds(
        Set.of(event.getPlayerOne(), event.getPlayerTwo())).keySet();

    for (final var seat : List.of(PlayerSeat.PLAYER_ONE, PlayerSeat.PLAYER_TWO)) {
      final var playerId = event.resolvePlayer(seat);
      if (!registeredPlayers.contains(playerId)) {
        continue;
      }
      if (!this.processedMatchStatsRegistry.tryRegister(playerId, event.getMatchId())) {
        continue;
      }
      final var outcome = seat == winnerSeat ? MatchOutcome.WON : MatchOutcome.LOST;
      final var stats = this.playerStatsRepository.findByPlayerId(playerId).orElseThrow();
      stats.recordOutcome(outcome);
      this.playerStatsRepository.save(stats);
    }
  }

  private PlayerSeat resolveWinnerSeat(final MatchDomainEvent event) {

    return switch (event) {
      case MatchFinishedEvent e -> e.getWinnerSeat();
      case MatchForfeitedEvent e -> e.getWinnerSeat();
      case MatchAbandonedEvent e -> e.getWinnerSeat();
      default -> null;
    };
  }

}
