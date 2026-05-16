package com.villo.truco.profile.infrastructure.persistence.repositories.spring;

import com.villo.truco.profile.infrastructure.persistence.entities.ProcessedMatchStatsId;
import com.villo.truco.profile.infrastructure.persistence.entities.ProcessedMatchStatsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProcessedMatchStatsRepository extends
    JpaRepository<ProcessedMatchStatsJpaEntity, ProcessedMatchStatsId> {

}
