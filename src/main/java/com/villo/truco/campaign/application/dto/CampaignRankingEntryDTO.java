package com.villo.truco.campaign.application.dto;

public record CampaignRankingEntryDTO(int position, String participantId, String displayName,
                                      int points, boolean player, boolean challengeable,
                                      CampaignRivalRecordDTO record) {

}
