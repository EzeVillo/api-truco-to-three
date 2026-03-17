package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.DomainEventBase;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

public interface MatchEventNotifier {

  void publishDomainEvents(MatchId matchId, PlayerId playerOne, PlayerId playerTwo,
      List<DomainEventBase> events);

}
