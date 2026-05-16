package com.villo.truco.auth.domain.ports;

import com.villo.truco.auth.domain.model.user.events.AuthDomainEvent;
import java.util.List;

public interface AuthEventNotifier {

  void publishDomainEvents(List<AuthDomainEvent> events);

}
