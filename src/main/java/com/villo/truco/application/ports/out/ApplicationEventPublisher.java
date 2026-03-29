package com.villo.truco.application.ports.out;

import com.villo.truco.application.events.ApplicationEvent;

public interface ApplicationEventPublisher {

  void publish(ApplicationEvent event);

}
