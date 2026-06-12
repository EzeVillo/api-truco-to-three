package com.villo.truco.campaign.infrastructure.persistence.repositories;

import com.villo.truco.campaign.domain.model.CampaignProgress;
import com.villo.truco.campaign.domain.ports.CampaignProgressRepository;
import com.villo.truco.campaign.infrastructure.persistence.mappers.CampaignProgressMapper;
import com.villo.truco.campaign.infrastructure.persistence.repositories.spring.SpringDataCampaignProgressRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.exceptions.StaleAggregateException;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaCampaignProgressRepositoryAdapter implements CampaignProgressRepository {

  private final SpringDataCampaignProgressRepository springDataCampaignProgressRepository;
  private final CampaignProgressMapper campaignProgressMapper;

  public JpaCampaignProgressRepositoryAdapter(
      final SpringDataCampaignProgressRepository springDataCampaignProgressRepository,
      final CampaignProgressMapper campaignProgressMapper) {

    this.springDataCampaignProgressRepository = springDataCampaignProgressRepository;
    this.campaignProgressMapper = campaignProgressMapper;
  }

  @Override
  @Transactional
  public void save(final CampaignProgress progress) {

    try {
      final var entity = this.campaignProgressMapper.toEntity(progress);
      this.springDataCampaignProgressRepository.saveAndFlush(entity);
      progress.setVersion(entity.getVersion());
    } catch (final ObjectOptimisticLockingFailureException ex) {
      throw new StaleAggregateException(
          "CampaignProgress " + progress.getId() + " was modified concurrently", ex);
    }
  }

  @Override
  public Optional<CampaignProgress> findByPlayerId(final PlayerId playerId) {

    return this.springDataCampaignProgressRepository.findById(playerId.value())
        .map(this.campaignProgressMapper::toDomain);
  }

}
