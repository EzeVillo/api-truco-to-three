package com.villo.truco.campaign.domain.model;

import com.villo.truco.campaign.domain.model.valueobjects.CampaignChallenge;
import com.villo.truco.campaign.domain.model.valueobjects.CampaignPoints;
import com.villo.truco.campaign.domain.model.valueobjects.CampaignRivalRecord;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Map;

public record CampaignProgressSnapshot(PlayerId playerId, CampaignPoints points,
                                       CampaignChallenge activeChallenge,
                                       Map<PlayerId, CampaignRivalRecord> rivalRecords,
                                       boolean topOneReached, boolean allRivalsDefeated) {

}
