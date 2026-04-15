package com.villo.truco.social.application.services;

import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.application.dto.FriendSummaryDTO;
import com.villo.truco.social.application.dto.IncomingFriendshipRequestDTO;
import com.villo.truco.social.application.dto.OutgoingFriendshipRequestDTO;
import com.villo.truco.social.application.dto.ResourceInvitationDTO;
import com.villo.truco.social.domain.model.friendship.Friendship;
import com.villo.truco.social.domain.model.invitation.ResourceInvitation;
import java.util.Objects;
import java.util.Set;

public final class SocialViewAssembler {

  private final UserQueryRepository userQueryRepository;

  public SocialViewAssembler(final UserQueryRepository userQueryRepository) {

    this.userQueryRepository = Objects.requireNonNull(userQueryRepository);
  }

  public FriendSummaryDTO toFriendSummaryDto(final Friendship friendship,
      final PlayerId perspective) {

    return new FriendSummaryDTO(this.resolveUsername(friendship.counterpartOf(perspective)));
  }

  public IncomingFriendshipRequestDTO toIncomingFriendshipRequestDto(final Friendship friendship) {

    return new IncomingFriendshipRequestDTO(this.resolveUsername(friendship.getRequesterId()));
  }

  public OutgoingFriendshipRequestDTO toOutgoingFriendshipRequestDto(final Friendship friendship) {

    return new OutgoingFriendshipRequestDTO(this.resolveUsername(friendship.getAddresseeId()));
  }

  public ResourceInvitationDTO toInvitationDto(final ResourceInvitation invitation) {

    final var usernames = this.userQueryRepository.findUsernamesByIds(
        Set.of(invitation.getSenderId(), invitation.getRecipientId()));
    return new ResourceInvitationDTO(invitation.getId().value().toString(),
        usernames.get(invitation.getSenderId()), usernames.get(invitation.getRecipientId()),
        invitation.getTargetType().name(), invitation.getTargetId(), invitation.getStatus().name(),
        invitation.getExpiresAt().toEpochMilli());
  }

  private String resolveUsername(final PlayerId playerId) {

    return this.userQueryRepository.findUsernamesByIds(Set.of(playerId)).get(playerId);
  }

}
