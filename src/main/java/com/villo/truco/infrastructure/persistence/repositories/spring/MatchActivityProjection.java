package com.villo.truco.infrastructure.persistence.repositories.spring;

import java.time.Instant;
import java.util.UUID;

public interface MatchActivityProjection {

  UUID getId();

  Instant getLastActivityAt();

}
