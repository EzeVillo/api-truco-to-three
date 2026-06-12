package com.villo.truco.campaign.domain.model.events;

import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public abstract class CampaignDomainEvent extends DomainEventBase {

  private final PlayerId playerId;

  protected CampaignDomainEvent(final String eventType, final PlayerId playerId) {

    super(eventType);
    this.playerId = Objects.requireNonNull(playerId);
  }

  public PlayerId getPlayerId() {

    return this.playerId;
  }

}
