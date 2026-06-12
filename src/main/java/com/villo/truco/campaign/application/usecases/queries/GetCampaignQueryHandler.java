package com.villo.truco.campaign.application.usecases.queries;

import com.villo.truco.campaign.application.dto.CampaignDTO;
import com.villo.truco.campaign.application.dto.CampaignRankingEntryDTO;
import com.villo.truco.campaign.application.dto.CampaignRivalRecordDTO;
import com.villo.truco.campaign.domain.model.CampaignLadder;
import com.villo.truco.campaign.domain.model.CampaignProgress;
import com.villo.truco.campaign.domain.model.valueobjects.CampaignRivalRecord;
import com.villo.truco.campaign.domain.ports.CampaignLadderProvider;
import com.villo.truco.campaign.domain.ports.CampaignProgressRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GetCampaignQueryHandler implements GetCampaignUseCase {

  private final CampaignProgressRepository campaignProgressRepository;
  private final CampaignLadderProvider campaignLadderProvider;

  public GetCampaignQueryHandler(final CampaignProgressRepository campaignProgressRepository,
      final CampaignLadderProvider campaignLadderProvider) {

    this.campaignProgressRepository = Objects.requireNonNull(campaignProgressRepository);
    this.campaignLadderProvider = Objects.requireNonNull(campaignLadderProvider);
  }

  @Override
  public CampaignDTO handle(final GetCampaignQuery query) {

    final var ladder = this.campaignLadderProvider.ladder();
    final var progress = this.campaignProgressRepository.findByPlayerId(query.playerId())
        .orElseGet(() -> CampaignProgress.create(query.playerId()));

    final var playerPosition = ladder.positionFor(progress.getPoints());
    final var activeChallenge = progress.getActiveChallenge();
    final var defeatedRivals = (int) progress.getRivalRecords().values().stream()
        .filter(CampaignRivalRecord::hasWin).count();
    final var pointsToNextPosition = ladder.pointsToOvertakeNextRival(progress.getPoints());

    final var ranking = this.buildRanking(query, progress, ladder, playerPosition);

    return new CampaignDTO(playerPosition, progress.getPoints().value(), ladder.totalBots(),
        defeatedRivals, progress.isTopOneReached(), progress.isAllRivalsDefeated(),
        pointsToNextPosition.isEmpty() ? null : pointsToNextPosition.getAsInt(),
        activeChallenge == null ? null : activeChallenge.matchId().value().toString(), ranking);
  }

  private List<CampaignRankingEntryDTO> buildRanking(final GetCampaignQuery query,
      final CampaignProgress progress, final CampaignLadder ladder, final int playerPosition) {

    final var ranking = new ArrayList<CampaignRankingEntryDTO>(ladder.totalBots() + 1);

    for (final var bot : ladder.bots()) {
      if (ranking.size() + 1 == playerPosition) {
        ranking.add(this.playerEntry(query, progress, playerPosition));
      }
      final var challengeable = progress.canChallenge(bot, ladder);
      final var record = progress.getRivalRecords().get(bot.playerId());
      ranking.add(new CampaignRankingEntryDTO(ranking.size() + 1, bot.playerId().value().toString(),
          bot.displayName(), bot.points(), false, challengeable,
          record == null ? null : new CampaignRivalRecordDTO(record.wins(), record.losses())));
    }

    if (ranking.size() + 1 == playerPosition) {
      ranking.add(this.playerEntry(query, progress, playerPosition));
    }

    return List.copyOf(ranking);
  }

  private CampaignRankingEntryDTO playerEntry(final GetCampaignQuery query,
      final CampaignProgress progress, final int playerPosition) {

    return new CampaignRankingEntryDTO(playerPosition, query.playerId().value().toString(), null,
        progress.getPoints().value(), true, false, null);
  }

}
