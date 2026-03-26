package com.villo.truco.infrastructure.events;

import com.villo.truco.application.ports.out.ChatDomainEventHandler;
import com.villo.truco.application.ports.out.ChatEventContext;
import com.villo.truco.domain.model.chat.valueobjects.ChatId;
import com.villo.truco.domain.ports.ChatEventNotifier;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Set;

public final class CompositeChatEventNotifier
    extends CompositeEventDispatcher<ChatEventContext>
    implements ChatEventNotifier {

    public CompositeChatEventNotifier(final List<ChatDomainEventHandler<?>> handlers) {

        super(handlers);
    }

    @Override
    public void publishDomainEvents(final ChatId chatId, final Set<PlayerId> participants,
        final List<DomainEventBase> events) {

        this.dispatchEvents(new ChatEventContext(chatId, Set.copyOf(participants)), events);
    }

}
