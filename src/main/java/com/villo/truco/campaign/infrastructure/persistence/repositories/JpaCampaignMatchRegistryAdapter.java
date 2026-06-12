package com.villo.truco.campaign.infrastructure.persistence.repositories;

import com.villo.truco.campaign.domain.ports.CampaignMatchRegistry;
import com.villo.truco.campaign.infrastructure.persistence.entities.CampaignMatchJpaEntity;
import com.villo.truco.campaign.infrastructure.persistence.repositories.spring.SpringDataCampaignMatchRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaCampaignMatchRegistryAdapter implements CampaignMatchRegistry {

  private final SpringDataCampaignMatchRepository springDataCampaignMatchRepository;

  public JpaCampaignMatchRegistryAdapter(
      final SpringDataCampaignMatchRepository springDataCampaignMatchRepository) {

    this.springDataCampaignMatchRepository = springDataCampaignMatchRepository;
  }

  @Override
  @Transactional
  public void register(final MatchId matchId, final PlayerId playerId) {

    final var entity = new CampaignMatchJpaEntity();
    entity.setMatchId(matchId.value());
    entity.setPlayerId(playerId.value());
    this.springDataCampaignMatchRepository.save(entity);
  }

  @Override
  public boolean isCampaignMatch(final MatchId matchId) {

    return this.springDataCampaignMatchRepository.existsById(matchId.value());
  }

  @Override
  public Optional<PlayerId> findPlayerByMatchId(final MatchId matchId) {

    return this.springDataCampaignMatchRepository.findById(matchId.value())
        .map(entity -> new PlayerId(entity.getPlayerId()));
  }

}
