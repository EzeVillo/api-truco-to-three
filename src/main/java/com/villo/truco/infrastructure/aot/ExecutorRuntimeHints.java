package com.villo.truco.infrastructure.aot;

import java.util.List;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

public final class ExecutorRuntimeHints implements RuntimeHintsRegistrar {

  private static final List<String> EXECUTOR_CLASSES = List.of(
      "java.util.concurrent.ThreadPerTaskExecutor", "java.util.concurrent.ThreadPoolExecutor",
      "java.util.concurrent.ScheduledThreadPoolExecutor",
      "java.util.concurrent.Executors$DelegatedExecutorService",
      "java.util.concurrent.Executors$FinalizableDelegatedExecutorService",
      "java.util.concurrent.Executors$DelegatedScheduledExecutorService");

  @Override
  public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {

    for (final var executorClass : EXECUTOR_CLASSES) {
      hints.reflection()
          .registerType(TypeReference.of(executorClass), MemberCategory.INVOKE_DECLARED_METHODS);
    }
  }

}
