package com.villo.truco.campaign.infrastructure.persistence.repositories.spring;

import com.villo.truco.campaign.infrastructure.persistence.entities.CampaignProgressJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCampaignProgressRepository extends
    JpaRepository<CampaignProgressJpaEntity, UUID> {

}
