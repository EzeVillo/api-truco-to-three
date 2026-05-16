package com.villo.truco.profile.application.usecases.queries;

import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.profile.application.dto.PlayerProfileDTO;
import com.villo.truco.profile.application.dto.PlayerStatsDTO;
import com.villo.truco.profile.application.dto.UnlockedAchievementDTO;
import com.villo.truco.profile.application.exceptions.PlayerNotFoundException;
import com.villo.truco.profile.domain.ports.PlayerProfileRepository;
import com.villo.truco.profile.domain.ports.PlayerStatsRepository;
import java.util.Objects;

public final class GetPlayerProfileQueryHandler implements GetPlayerProfileUseCase {

  private final UserQueryRepository userQueryRepository;
  private final PlayerProfileRepository playerProfileRepository;
  private final PlayerStatsRepository playerStatsRepository;

  public GetPlayerProfileQueryHandler(final UserQueryRepository userQueryRepository,
      final PlayerProfileRepository playerProfileRepository,
      final PlayerStatsRepository playerStatsRepository) {

    this.userQueryRepository = Objects.requireNonNull(userQueryRepository);
    this.playerProfileRepository = Objects.requireNonNull(playerProfileRepository);
    this.playerStatsRepository = Objects.requireNonNull(playerStatsRepository);
  }

  @Override
  public PlayerProfileDTO handle(final GetPlayerProfileQuery query) {

    final var playerId = this.userQueryRepository.findUserIdByUsername(query.username())
        .orElseThrow(() -> new PlayerNotFoundException(query.username()));

    final var profile = this.playerProfileRepository.findByPlayerId(playerId)
        .orElseThrow(() -> new PlayerNotFoundException(query.username()));
    final var stats = this.playerStatsRepository.findByPlayerId(playerId)
        .orElseThrow(() -> new PlayerNotFoundException(query.username()));

    final var achievements = profile.getUnlockedAchievements().stream().map(
        a -> new UnlockedAchievementDTO(a.achievementCode(), a.unlockedAt(), a.matchId().value(),
            a.gameNumber())).toList();

    final var statsDTO = new PlayerStatsDTO(stats.matchesPlayed(), stats.matchesWon(),
        stats.matchesLost(), stats.winRate());

    return new PlayerProfileDTO(query.username(), achievements, statsDTO);
  }

}
