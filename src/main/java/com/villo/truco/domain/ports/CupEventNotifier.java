package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.cup.events.CupDomainEvent;
import java.util.List;

public interface CupEventNotifier {

  void publishDomainEvents(List<CupDomainEvent> events);

}
