package com.villo.truco.infrastructure.aot;

import java.util.List;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.util.ClassUtils;

public final class WebSocketPayloadRuntimeHints implements RuntimeHintsRegistrar {

  private static final List<String> PAYLOAD_PACKAGES = List.of("com.villo.truco.application.dto",
      "com.villo.truco.profile.application.dto", "com.villo.truco.social.application.dto",
      "com.villo.truco.infrastructure.websocket.dto",
      "com.villo.truco.profile.infrastructure.websocket.dto",
      "com.villo.truco.social.infrastructure.websocket.dto");

  @Override
  public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {

    final var bindingRegistrar = new BindingReflectionHintsRegistrar();
    final var scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);
    for (final var payloadPackage : PAYLOAD_PACKAGES) {
      for (final var candidate : scanner.findCandidateComponents(payloadPackage)) {
        final var dtoClass = ClassUtils.resolveClassName(candidate.getBeanClassName(), classLoader);
        bindingRegistrar.registerReflectionHints(hints.reflection(), dtoClass);
      }
    }
  }

}
