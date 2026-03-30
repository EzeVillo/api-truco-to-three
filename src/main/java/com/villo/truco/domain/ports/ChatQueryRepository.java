package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.chat.Chat;
import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import java.util.Optional;

public interface ChatQueryRepository {

  Optional<Chat> findById(ChatId chatId);

  Optional<Chat> findByParentTypeAndParentId(ChatParentType parentType, String parentId);

}
