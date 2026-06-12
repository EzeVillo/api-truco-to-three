package com.villo.truco.campaign.application.support;

import com.villo.truco.campaign.domain.ports.CampaignMatchRegistry;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryCampaignMatchRegistry implements CampaignMatchRegistry {

  private final Map<MatchId, PlayerId> store = new ConcurrentHashMap<>();

  @Override
  public void register(final MatchId matchId, final PlayerId playerId) {

    this.store.put(matchId, playerId);
  }

  @Override
  public boolean isCampaignMatch(final MatchId matchId) {

    return this.store.containsKey(matchId);
  }

  @Override
  public Optional<PlayerId> findPlayerByMatchId(final MatchId matchId) {

    return Optional.ofNullable(this.store.get(matchId));
  }

}
