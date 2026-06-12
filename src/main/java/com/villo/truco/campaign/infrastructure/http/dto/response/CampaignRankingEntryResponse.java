package com.villo.truco.campaign.infrastructure.http.dto.response;

import com.villo.truco.campaign.application.dto.CampaignRankingEntryDTO;

public record CampaignRankingEntryResponse(int position, String participantId, String displayName,
                                           int points, boolean player, boolean challengeable,
                                           CampaignRivalRecordResponse record) {

  public static CampaignRankingEntryResponse from(final CampaignRankingEntryDTO dto) {

    return new CampaignRankingEntryResponse(dto.position(), dto.participantId(), dto.displayName(),
        dto.points(), dto.player(), dto.challengeable(), dto.record() == null ? null
        : new CampaignRivalRecordResponse(dto.record().wins(), dto.record().losses()));
  }

}
