package com.villo.truco.application.ports;

import com.villo.truco.domain.shared.valueobjects.PlayerId;

public interface PublicActorResolver {

  String resolve(PlayerId playerId);

}
