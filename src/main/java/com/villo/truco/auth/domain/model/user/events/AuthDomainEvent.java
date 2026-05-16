package com.villo.truco.auth.domain.model.user.events;

import com.villo.truco.domain.shared.DomainEventBase;

public abstract class AuthDomainEvent extends DomainEventBase {

  protected AuthDomainEvent(final String eventType) {

    super(eventType);
  }

}
