package com.villo.truco.application.queries;

import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record GetChatByParentQuery(ChatParentType parentType, String parentId,
                                   PlayerId requestingPlayer) {

    public GetChatByParentQuery {

        Objects.requireNonNull(parentType);
        Objects.requireNonNull(parentId);
        Objects.requireNonNull(requestingPlayer);
    }

    public GetChatByParentQuery(final String parentType, final String parentId,
        final String requestingPlayer) {

        this(ChatParentType.valueOf(parentType), parentId, PlayerId.of(requestingPlayer));
    }

}
