package com.villo.truco.social.domain.ports;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.invitation.ResourceInvitation;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationId;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;
import java.util.List;
import java.util.Optional;

public interface ResourceInvitationQueryRepository {

  Optional<ResourceInvitation> findById(ResourceInvitationId invitationId);

  List<ResourceInvitation> findPendingReceivedBy(PlayerId playerId);

  List<ResourceInvitation> findPendingSentBy(PlayerId playerId);

  List<ResourceInvitation> findPendingInvitations();

  boolean existsPendingBySenderAndRecipientAndTarget(PlayerId senderId, PlayerId recipientId,
      ResourceInvitationTargetType targetType, String targetId);

  List<ResourceInvitation> findPendingByTarget(ResourceInvitationTargetType targetType,
      String targetId);

}
