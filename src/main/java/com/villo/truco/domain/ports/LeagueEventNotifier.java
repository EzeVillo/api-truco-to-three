package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.league.events.LeagueDomainEvent;
import java.util.List;

public interface LeagueEventNotifier {

  void publishDomainEvents(List<LeagueDomainEvent> events);

}
