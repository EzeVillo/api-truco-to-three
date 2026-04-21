package com.villo.truco.social.application.commands;

import com.villo.truco.application.shared.EnumArgumentParser;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;
import java.util.Objects;

public record CreateResourceInvitationCommand(PlayerId senderId, String recipientUsername,
                                              ResourceInvitationTargetType targetType,
                                              String targetId) {

  public CreateResourceInvitationCommand {

    Objects.requireNonNull(senderId);
    Objects.requireNonNull(recipientUsername);
    Objects.requireNonNull(targetType);
    Objects.requireNonNull(targetId);
  }

  public CreateResourceInvitationCommand(final String senderId, final String recipientUsername,
      final String targetType, final String targetId) {

    this(PlayerId.of(senderId), recipientUsername,
        EnumArgumentParser.parse(ResourceInvitationTargetType.class, "targetType", targetType),
        targetId);
  }

}
