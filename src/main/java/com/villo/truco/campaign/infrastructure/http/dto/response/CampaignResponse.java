package com.villo.truco.campaign.infrastructure.http.dto.response;

import com.villo.truco.campaign.application.dto.CampaignDTO;
import java.util.List;

public record CampaignResponse(int playerPosition, int playerPoints, int totalBots,
                               int defeatedRivals, boolean topOneReached, boolean allRivalsDefeated,
                               Integer pointsToNextPosition, String activeChallengeMatchId,
                               List<CampaignRankingEntryResponse> ranking) {

  public static CampaignResponse from(final CampaignDTO dto) {

    return new CampaignResponse(dto.playerPosition(), dto.playerPoints(), dto.totalBots(),
        dto.defeatedRivals(), dto.topOneReached(), dto.allRivalsDefeated(),
        dto.pointsToNextPosition(), dto.activeChallengeMatchId(),
        dto.ranking().stream().map(CampaignRankingEntryResponse::from).toList());
  }

}
