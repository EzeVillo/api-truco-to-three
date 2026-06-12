package com.villo.truco.campaign.application.usecases.commands;

import com.villo.truco.application.ports.in.UseCase;
import com.villo.truco.campaign.application.dto.StartCampaignChallengeDTO;

public interface StartCampaignChallengeUseCase extends
    UseCase<StartCampaignChallengeCommand, StartCampaignChallengeDTO> {

}
