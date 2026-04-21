package com.villo.truco.social.domain.ports;

import com.villo.truco.social.domain.model.friendship.events.SocialDomainEvent;
import java.util.List;

public interface SocialEventNotifier {

  void publishDomainEvents(List<? extends SocialDomainEvent> events);

}
