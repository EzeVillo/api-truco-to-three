package com.villo.truco.campaign.application.support;

import com.villo.truco.campaign.domain.model.events.CampaignDomainEvent;
import com.villo.truco.campaign.domain.ports.CampaignEventNotifier;
import java.util.ArrayList;
import java.util.List;

public final class RecordingCampaignEventNotifier implements CampaignEventNotifier {

  private final List<CampaignDomainEvent> published = new ArrayList<>();

  @Override
  public void publishDomainEvents(final List<CampaignDomainEvent> events) {

    this.published.addAll(events);
  }

  public List<CampaignDomainEvent> published() {

    return List.copyOf(this.published);
  }

}
