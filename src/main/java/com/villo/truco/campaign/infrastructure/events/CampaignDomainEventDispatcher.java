package com.villo.truco.campaign.infrastructure.events;

import com.villo.truco.campaign.application.ports.out.CampaignDomainEventHandler;
import com.villo.truco.campaign.domain.model.events.CampaignDomainEvent;
import com.villo.truco.campaign.domain.ports.CampaignEventNotifier;
import com.villo.truco.infrastructure.events.CompositeEventDispatcher;
import java.util.List;

public final class CampaignDomainEventDispatcher extends CompositeEventDispatcher implements
    CampaignEventNotifier {

  public CampaignDomainEventDispatcher(final List<CampaignDomainEventHandler<?>> handlers) {

    super(handlers);
  }

  @Override
  public void publishDomainEvents(final List<CampaignDomainEvent> events) {

    this.dispatchEvents(events);
  }

}
