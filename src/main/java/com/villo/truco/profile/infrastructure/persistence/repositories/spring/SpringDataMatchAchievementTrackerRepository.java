package com.villo.truco.profile.infrastructure.persistence.repositories.spring;

import com.villo.truco.profile.infrastructure.persistence.entities.MatchAchievementTrackerJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataMatchAchievementTrackerRepository extends
    JpaRepository<MatchAchievementTrackerJpaEntity, UUID> {

}
