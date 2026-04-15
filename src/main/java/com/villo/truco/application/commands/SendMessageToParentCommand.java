package com.villo.truco.application.commands;

import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record SendMessageToParentCommand(ChatParentType parentType, String parentId,
                                         PlayerId playerId, String content) {

  public SendMessageToParentCommand {

    Objects.requireNonNull(parentType);
    Objects.requireNonNull(parentId);
    Objects.requireNonNull(playerId);
    Objects.requireNonNull(content);
  }

  public SendMessageToParentCommand(final String parentType, final String parentId,
      final String playerId, final String content) {

    this(ChatParentType.valueOf(parentType), parentId, PlayerId.of(playerId), content);
  }

}
