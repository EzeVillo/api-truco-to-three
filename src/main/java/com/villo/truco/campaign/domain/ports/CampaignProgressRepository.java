package com.villo.truco.campaign.domain.ports;

import com.villo.truco.campaign.domain.model.CampaignProgress;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;

public interface CampaignProgressRepository {

  void save(CampaignProgress progress);

  Optional<CampaignProgress> findByPlayerId(PlayerId playerId);

}
