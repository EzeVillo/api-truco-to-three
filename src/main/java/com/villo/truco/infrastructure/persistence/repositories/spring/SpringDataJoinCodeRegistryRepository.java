package com.villo.truco.infrastructure.persistence.repositories.spring;

import com.villo.truco.infrastructure.persistence.entities.JoinCodeRegistryJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataJoinCodeRegistryRepository extends
    JpaRepository<JoinCodeRegistryJpaEntity, String> {

  @Modifying
  @Query(value = "INSERT INTO join_code_registry (join_code, target_type, target_id) "
      + "VALUES (:joinCode, :targetType, :targetId) ON CONFLICT (join_code) DO NOTHING", nativeQuery = true)
  int insertIfAbsent(@Param("joinCode") String joinCode, @Param("targetType") String targetType,
      @Param("targetId") UUID targetId);

}
