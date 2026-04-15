package com.villo.truco.infrastructure.persistence.repositories.spring;

import com.villo.truco.infrastructure.persistence.entities.JoinCodeRegistryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJoinCodeRegistryRepository extends
    JpaRepository<JoinCodeRegistryJpaEntity, String> {

}
