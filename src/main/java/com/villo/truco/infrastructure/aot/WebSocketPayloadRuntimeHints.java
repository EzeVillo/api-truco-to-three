package com.villo.truco.infrastructure.aot;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.util.ClassUtils;

public final class WebSocketPayloadRuntimeHints implements RuntimeHintsRegistrar {

  private static final String BASE_PACKAGE = "com.villo.truco";

  private static boolean isDtoPackage(final String className) {

    final var packageName = className.substring(0, className.lastIndexOf('.'));
    return packageName.endsWith(".dto") || packageName.contains(".dto.");
  }

  @Override
  public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {

    final var bindingRegistrar = new BindingReflectionHintsRegistrar();
    final var scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);
    for (final var candidate : scanner.findCandidateComponents(BASE_PACKAGE)) {
      final var className = candidate.getBeanClassName();
      if (isDtoPackage(className)) {
        bindingRegistrar.registerReflectionHints(hints.reflection(),
            ClassUtils.resolveClassName(className, classLoader));
      }
    }
  }

}
