package com.villo.truco.campaign.application.dto;

import java.util.List;

public record CampaignDTO(int playerPosition, int playerPoints, int totalBots, int defeatedRivals,
                          boolean topOneReached, boolean allRivalsDefeated,
                          Integer pointsToNextPosition, String activeChallengeMatchId,
                          List<CampaignRankingEntryDTO> ranking) {

}
