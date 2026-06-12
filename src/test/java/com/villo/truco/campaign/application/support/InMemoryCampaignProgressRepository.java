package com.villo.truco.campaign.application.support;

import com.villo.truco.campaign.domain.model.CampaignProgress;
import com.villo.truco.campaign.domain.ports.CampaignProgressRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryCampaignProgressRepository implements CampaignProgressRepository {

  private final Map<PlayerId, CampaignProgress> store = new ConcurrentHashMap<>();

  @Override
  public void save(final CampaignProgress progress) {

    this.store.put(progress.getId(), progress);
  }

  @Override
  public Optional<CampaignProgress> findByPlayerId(final PlayerId playerId) {

    return Optional.ofNullable(this.store.get(playerId));
  }

}
