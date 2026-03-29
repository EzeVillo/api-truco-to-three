package com.villo.truco.application.ports.out;

import com.villo.truco.application.events.ApplicationEvent;

public interface ApplicationEventHandler<E extends ApplicationEvent> {

  Class<E> eventType();

  void handle(E event);

}
