package com.villo.truco.application.eventhandlers;

import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutKey;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeoutActionDispatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutActionDispatcher.class);

  private final Map<EntityType, Function<String, Runnable>> registry = new EnumMap<>(
      EntityType.class);

  public void register(final EntityType type, final Function<String, Runnable> actionFactory) {

    registry.put(type, actionFactory);
  }

  public Runnable buildAction(final TimeoutKey key) {

    final var factory = registry.get(key.type());
    if (factory == null) {
      LOGGER.warn("Sin handler para timeout: entityType={}", key.type());
      return () -> {
      };
    }
    return factory.apply(key.entityId());
  }

}
