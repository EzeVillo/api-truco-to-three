package com.villo.truco.social.infrastructure.persistence.repositories.spring;

import com.villo.truco.social.infrastructure.persistence.entities.ResourceInvitationJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataResourceInvitationRepository extends
    JpaRepository<ResourceInvitationJpaEntity, UUID> {

  List<ResourceInvitationJpaEntity> findByRecipientIdAndStatusOrderByExpiresAtAsc(UUID recipientId,
      String status);

  List<ResourceInvitationJpaEntity> findBySenderIdAndStatusOrderByExpiresAtAsc(UUID senderId,
      String status);

  List<ResourceInvitationJpaEntity> findByStatusOrderByExpiresAtAsc(String status);

  boolean existsBySenderIdAndRecipientIdAndTargetTypeAndTargetIdAndStatus(UUID senderId,
      UUID recipientId, String targetType, UUID targetId, String status);

  List<ResourceInvitationJpaEntity> findByTargetTypeAndTargetIdAndStatus(String targetType,
      UUID targetId, String status);

}
