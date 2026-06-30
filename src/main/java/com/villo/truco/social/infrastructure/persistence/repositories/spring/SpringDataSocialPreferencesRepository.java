package com.villo.truco.social.infrastructure.persistence.repositories.spring;

import com.villo.truco.social.infrastructure.persistence.entities.SocialPreferencesJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataSocialPreferencesRepository extends
    JpaRepository<SocialPreferencesJpaEntity, UUID> {

}
