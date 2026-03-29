package com.villo.truco.domain.model.chat;

import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Set;

public record ChatReadView(ChatId id, ChatParentType parentType, String parentId,
                           Set<PlayerId> participants, List<ChatMessageView> messages) {

}
