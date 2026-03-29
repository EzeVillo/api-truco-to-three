package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.match.events.MatchDomainEvent;
import java.util.List;

public interface MatchEventNotifier {

  void publishDomainEvents(List<MatchDomainEvent> events);

}
