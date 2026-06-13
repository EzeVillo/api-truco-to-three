package com.villo.truco.infrastructure.persistence.repositories.spring;

import java.time.Instant;
import java.util.UUID;

public interface LeagueActivityProjection {

  UUID getId();

  Instant getLastActivityAt();

  String getStatus();

}
