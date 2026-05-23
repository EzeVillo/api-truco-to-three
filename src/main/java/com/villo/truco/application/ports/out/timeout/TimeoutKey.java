package com.villo.truco.application.ports.out.timeout;

import java.util.Objects;

public record TimeoutKey(EntityType type, String entityId) {

  public TimeoutKey {

    Objects.requireNonNull(type, "EntityType cannot be null");
    Objects.requireNonNull(entityId, "entityId cannot be null");
  }

  public static TimeoutKey of(final EntityType type, final String entityId) {

    return new TimeoutKey(type, entityId);
  }

}
