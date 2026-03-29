package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.chat.events.ChatDomainEvent;
import java.util.List;

public interface ChatEventNotifier {

  void publishDomainEvents(List<ChatDomainEvent> events);

}
