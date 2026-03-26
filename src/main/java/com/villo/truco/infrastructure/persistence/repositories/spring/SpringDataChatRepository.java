package com.villo.truco.infrastructure.persistence.repositories.spring;

import com.villo.truco.infrastructure.persistence.entities.ChatJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataChatRepository extends JpaRepository<ChatJpaEntity, UUID> {

    Optional<ChatJpaEntity> findByParentTypeAndParentId(String parentType, String parentId);

}
