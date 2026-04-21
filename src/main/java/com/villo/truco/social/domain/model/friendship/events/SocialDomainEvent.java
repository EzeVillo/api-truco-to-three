package com.villo.truco.social.domain.model.friendship.events;

import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;

public abstract class SocialDomainEvent extends DomainEventBase {

  private final List<PlayerId> recipients;

  protected SocialDomainEvent(final String eventType, final List<PlayerId> recipients) {

    super(eventType);
    final var copy = List.copyOf(Objects.requireNonNull(recipients));
    if (copy.isEmpty()) {
      throw new IllegalArgumentException("recipients must not be empty");
    }
    this.recipients = copy;
  }

  public List<PlayerId> getRecipients() {

    return this.recipients;
  }

}
