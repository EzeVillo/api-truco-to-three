package com.villo.truco.campaign.domain.ports;

import com.villo.truco.campaign.domain.model.events.CampaignDomainEvent;
import java.util.List;

public interface CampaignEventNotifier {

  void publishDomainEvents(List<CampaignDomainEvent> events);

}
