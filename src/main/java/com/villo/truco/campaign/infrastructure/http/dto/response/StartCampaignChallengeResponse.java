package com.villo.truco.campaign.infrastructure.http.dto.response;

import com.villo.truco.campaign.application.dto.StartCampaignChallengeDTO;

public record StartCampaignChallengeResponse(String matchId, String rivalId, String rivalName,
                                             int rivalPosition) {

  public static StartCampaignChallengeResponse from(final StartCampaignChallengeDTO dto) {

    return new StartCampaignChallengeResponse(dto.matchId(), dto.rivalId(), dto.rivalName(),
        dto.rivalPosition());
  }

}
