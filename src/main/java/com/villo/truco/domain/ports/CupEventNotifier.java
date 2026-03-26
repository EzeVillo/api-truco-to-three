package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

public interface CupEventNotifier {

    void publishDomainEvents(CupId cupId, List<PlayerId> participants,
        List<DomainEventBase> events);

}
