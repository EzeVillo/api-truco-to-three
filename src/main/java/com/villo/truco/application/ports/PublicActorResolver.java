package com.villo.truco.application.ports;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Collection;
import java.util.Map;

public interface PublicActorResolver {

  String resolve(PlayerId playerId);

  Map<PlayerId, String> resolveAll(final Collection<PlayerId> playerIds);

}
