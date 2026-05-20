package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.rematch.events.RematchSessionDomainEvent;
import java.util.List;

public interface RematchSessionEventNotifier {

  void publishDomainEvents(List<RematchSessionDomainEvent> events);

}
