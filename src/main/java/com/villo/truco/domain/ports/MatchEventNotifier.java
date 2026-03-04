package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import com.villo.truco.domain.shared.DomainEventBase;
import java.util.List;

public interface MatchEventNotifier {

  void publishDomainEvents(MatchId matchId, PlayerId playerOne, PlayerId playerTwo,
      List<DomainEventBase> events);

}
