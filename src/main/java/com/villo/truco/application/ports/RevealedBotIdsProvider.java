package com.villo.truco.application.ports;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Set;

public interface RevealedBotIdsProvider {

  Set<PlayerId> revealedBotIds(PlayerId playerId);

}
