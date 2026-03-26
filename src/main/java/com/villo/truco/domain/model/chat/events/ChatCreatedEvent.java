package com.villo.truco.domain.model.chat.events;

import com.villo.truco.domain.shared.DomainEventBase;

public final class ChatCreatedEvent extends DomainEventBase {

    public ChatCreatedEvent() {

        super("CHAT_CREATED");
    }

}
