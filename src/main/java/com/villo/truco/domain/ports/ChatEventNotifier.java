package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Set;

public interface ChatEventNotifier {

    void publishDomainEvents(ChatId chatId, Set<PlayerId> participants,
        List<DomainEventBase> events);

}
