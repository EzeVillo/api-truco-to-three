package com.villo.truco.campaign.application.support;

import com.villo.truco.campaign.domain.model.CampaignLadder;
import com.villo.truco.campaign.domain.ports.CampaignLadderProvider;

public record FixedCampaignLadderProvider(CampaignLadder ladder) implements CampaignLadderProvider {

}
