package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.chat.Chat;
import com.villo.truco.domain.model.chat.valueobjects.ChatId;

public interface ChatRepository {

    void save(Chat chat);

    void delete(ChatId chatId);

}
