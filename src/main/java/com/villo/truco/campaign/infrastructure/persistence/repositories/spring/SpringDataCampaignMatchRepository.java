package com.villo.truco.campaign.infrastructure.persistence.repositories.spring;

import com.villo.truco.campaign.infrastructure.persistence.entities.CampaignMatchJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCampaignMatchRepository extends
    JpaRepository<CampaignMatchJpaEntity, UUID> {

}
