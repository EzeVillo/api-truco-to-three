package com.villo.truco.infrastructure.aot;

import java.lang.reflect.Executable;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.ClassUtils;

public final class LazyInjectionProxyRuntimeHints implements RuntimeHintsRegistrar {

  private static final String BASE_PACKAGE = "com.villo.truco";

  @Override
  public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {

    final var scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);
    for (final var candidate : scanner.findCandidateComponents(BASE_PACKAGE)) {
      final var candidateClass = ClassUtils.resolveClassName(candidate.getBeanClassName(),
          classLoader);
      this.registerLazyParameterProxies(hints, candidateClass.getDeclaredConstructors());
      this.registerLazyParameterProxies(hints, candidateClass.getDeclaredMethods());
    }
  }

  private void registerLazyParameterProxies(final RuntimeHints hints,
      final Executable[] executables) {

    for (final var executable : executables) {
      for (final var parameter : executable.getParameters()) {
        if (parameter.isAnnotationPresent(Lazy.class) && parameter.getType().isInterface()) {
          hints.proxies()
              .registerJdkProxy(AopProxyUtils.completeJdkProxyInterfaces(parameter.getType()));
        }
      }
    }
  }

}
