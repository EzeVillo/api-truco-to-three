package com.villo.truco.history.application.usecases.queries;

import com.villo.truco.application.ports.BotRegistry;
import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.history.application.dto.MatchHistoryEntryDTO;
import com.villo.truco.history.application.dto.PlayerMatchHistoryDTO;
import com.villo.truco.history.domain.model.MatchHistoryEntry;
import com.villo.truco.history.domain.ports.PlayerMatchHistoryRepository;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class GetPlayerMatchHistoryQueryHandler implements GetPlayerMatchHistoryUseCase {

  private static final String UNKNOWN_OPPONENT = "Invitado";

  private final PlayerMatchHistoryRepository playerMatchHistoryRepository;
  private final UserQueryRepository userQueryRepository;
  private final BotRegistry botRegistry;

  public GetPlayerMatchHistoryQueryHandler(
      final PlayerMatchHistoryRepository playerMatchHistoryRepository,
      final UserQueryRepository userQueryRepository, final BotRegistry botRegistry) {

    this.playerMatchHistoryRepository = Objects.requireNonNull(playerMatchHistoryRepository);
    this.userQueryRepository = Objects.requireNonNull(userQueryRepository);
    this.botRegistry = Objects.requireNonNull(botRegistry);
  }

  @Override
  public PlayerMatchHistoryDTO handle(final GetPlayerMatchHistoryQuery query) {

    final var playerId = PlayerId.of(query.userId());
    final var history = this.playerMatchHistoryRepository.findByPlayerId(playerId);
    if (history.isEmpty()) {
      return new PlayerMatchHistoryDTO(List.of());
    }

    final var entries = history.get().getEntries();
    final var opponentIds = entries.stream().map(MatchHistoryEntry::opponentId)
        .collect(Collectors.toSet());
    final var usernames = this.userQueryRepository.findUsernamesByIds(opponentIds);

    final var dtos = entries.stream().map(entry -> toDto(entry, usernames.get(entry.opponentId())))
        .toList();
    return new PlayerMatchHistoryDTO(dtos);
  }

  private MatchHistoryEntryDTO toDto(final MatchHistoryEntry entry, final String username) {

    final var isBot = this.botRegistry.isBot(entry.opponentId());
    final var opponentName = resolveOpponentName(entry, username, isBot);
    return new MatchHistoryEntryDTO(entry.matchId().value(), opponentName, isBot,
        entry.outcome().name(), entry.endReason().name(), entry.ownGamesWon(),
        entry.opponentGamesWon(), entry.endedAt().toEpochMilli());
  }

  private String resolveOpponentName(final MatchHistoryEntry entry, final String username,
      final boolean isBot) {

    if (username != null) {
      return username;
    }
    if (isBot) {
      return this.botRegistry.getProfile(entry.opponentId()).map(BotProfile::displayName)
          .orElse(UNKNOWN_OPPONENT);
    }
    return UNKNOWN_OPPONENT;
  }

}
