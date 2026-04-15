package com.villo.truco.social.infrastructure.persistence.mappers;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.invitation.ResourceInvitation;
import com.villo.truco.social.domain.model.invitation.ResourceInvitationRehydrator;
import com.villo.truco.social.domain.model.invitation.ResourceInvitationSnapshot;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationId;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationStatus;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;
import com.villo.truco.social.infrastructure.persistence.entities.ResourceInvitationJpaEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ResourceInvitationMapper {

  public ResourceInvitationJpaEntity toEntity(final ResourceInvitation invitation) {

    final var entity = new ResourceInvitationJpaEntity();
    entity.setId(invitation.getId().value());
    entity.setSenderId(invitation.getSenderId().value());
    entity.setRecipientId(invitation.getRecipientId().value());
    entity.setTargetType(invitation.getTargetType().name());
    entity.setTargetId(UUID.fromString(invitation.getTargetId()));
    entity.setStatus(invitation.getStatus().name());
    entity.setExpiresAt(invitation.getExpiresAt());
    entity.setVersion((int) invitation.getVersion());
    return entity;
  }

  public ResourceInvitation toDomain(final ResourceInvitationJpaEntity entity) {

    final var invitation = ResourceInvitationRehydrator.rehydrate(
        new ResourceInvitationSnapshot(new ResourceInvitationId(entity.getId()),
            new PlayerId(entity.getSenderId()), new PlayerId(entity.getRecipientId()),
            ResourceInvitationTargetType.valueOf(entity.getTargetType()),
            entity.getTargetId().toString(), ResourceInvitationStatus.valueOf(entity.getStatus()),
            entity.getExpiresAt()));
    invitation.setVersion(entity.getVersion());
    return invitation;
  }

}
