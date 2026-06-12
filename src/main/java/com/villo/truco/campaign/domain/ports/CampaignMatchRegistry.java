package com.villo.truco.campaign.domain.ports;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;

public interface CampaignMatchRegistry {

  void register(MatchId matchId, PlayerId playerId);

  boolean isCampaignMatch(MatchId matchId);

  Optional<PlayerId> findPlayerByMatchId(MatchId matchId);

}
