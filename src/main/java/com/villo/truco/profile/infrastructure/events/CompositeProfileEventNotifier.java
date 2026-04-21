package com.villo.truco.profile.infrastructure.events;

import com.villo.truco.application.ports.out.DomainEventHandler;
import com.villo.truco.infrastructure.events.CompositeEventDispatcher;
import com.villo.truco.profile.domain.model.events.AchievementUnlocked;
import com.villo.truco.profile.domain.ports.ProfileEventNotifier;
import java.util.List;

public final class CompositeProfileEventNotifier extends CompositeEventDispatcher implements
    ProfileEventNotifier {

  public CompositeProfileEventNotifier(final List<? extends DomainEventHandler<?>> handlers) {

    super(handlers);
  }

  @Override
  public void publishDomainEvents(final List<? extends AchievementUnlocked> events) {

    this.dispatchEvents(events);
  }
}
