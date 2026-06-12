package com.villo.truco.campaign.application.ports.out;

import com.villo.truco.application.ports.out.DomainEventHandler;
import com.villo.truco.domain.shared.DomainEventBase;

public interface CampaignDomainEventHandler<E extends DomainEventBase> extends
    DomainEventHandler<E> {

}
