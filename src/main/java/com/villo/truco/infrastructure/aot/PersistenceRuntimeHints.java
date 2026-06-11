package com.villo.truco.infrastructure.aot;

import java.util.UUID;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public final class PersistenceRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {

    hints.reflection().registerType(UUID[].class);
  }

}
