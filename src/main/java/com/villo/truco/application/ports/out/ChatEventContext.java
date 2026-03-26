package com.villo.truco.application.ports.out;

import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Set;

public record ChatEventContext(ChatId chatId, Set<PlayerId> participants) {

}
