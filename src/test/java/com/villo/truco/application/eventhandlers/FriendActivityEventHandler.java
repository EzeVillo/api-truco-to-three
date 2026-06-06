package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.events.ApplicationEvent;
import com.villo.truco.application.ports.out.ApplicationEventPublisher;
import java.util.ArrayList;
import java.util.List;

public final class FriendActivityEventHandler implements ApplicationEventPublisher {

  private final List<ApplicationEvent> events = new ArrayList<>();

  @Override
  public void publish(final ApplicationEvent event) {

    this.events.add(event);
  }

  public List<ApplicationEvent> events() {

    return List.copyOf(this.events);
  }

}
