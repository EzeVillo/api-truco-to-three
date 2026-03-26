package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

public interface LeagueEventNotifier {

    void publishDomainEvents(LeagueId leagueId, List<PlayerId> participants,
        List<DomainEventBase> events);

}
