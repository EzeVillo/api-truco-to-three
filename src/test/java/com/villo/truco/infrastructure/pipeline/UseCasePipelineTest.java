package com.villo.truco.infrastructure.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.ports.in.PipelineBehavior;
import com.villo.truco.application.ports.in.UseCase;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UseCasePipeline")
class UseCasePipelineTest {

  @Test
  @DisplayName("sin behaviors delega directo al handler")
  void noBehaviorsDelegatesDirectly() {

    final var pipeline = new UseCasePipeline(List.of());
    final UseCase<String, String> handler = command -> "result:" + command;

    final var wrapped = pipeline.wrap(handler);

    assertThat(wrapped.handle("test")).isEqualTo("result:test");
  }

  @Test
  @DisplayName("un solo behavior envuelve la ejecución del handler")
  void singleBehaviorWrapsHandler() {

    final var log = new ArrayList<String>();

    final PipelineBehavior behavior = new PipelineBehavior() {
      @Override
      public <C, R> R handle(final C command, final Supplier<R> next) {

        log.add("before");
        final var result = next.get();
        log.add("after");
        return result;
      }
    };

    final var pipeline = new UseCasePipeline(List.of(behavior));
    final UseCase<String, String> handler = command -> {
      log.add("handler");
      return "done";
    };

    final var result = pipeline.wrap(handler).handle("cmd");

    assertThat(result).isEqualTo("done");
    assertThat(log).containsExactly("before", "handler", "after");
  }

  @Test
  @DisplayName("retry (outer) -> tx (inner): cada reintento abre una tx nueva")
  void retryOutsideTxInsideOpensNewTransactionPerAttempt() {

    final var txOpenCount = new ArrayList<String>();
    final var attemptCount = new int[]{0};

    final var pipeline = getUseCasePipeline(txOpenCount);
    final UseCase<String, String> handler = command -> {
      attemptCount[0]++;
      if (attemptCount[0] < 3) {
        throw new RuntimeException("conflict");
      }
      return "done";
    };

    final var result = pipeline.wrap(handler).handle("cmd");

    assertThat(result).isEqualTo("done");
    assertThat(attemptCount[0]).isEqualTo(3);
    // cada intento abre y cierra su propia tx
    assertThat(txOpenCount).containsExactly("tx-open", "tx-open", "tx-open", "tx-commit");
  }

  private UseCasePipeline getUseCasePipeline(final ArrayList<String> txOpenCount) {

    final PipelineBehavior retryBehavior = new PipelineBehavior() {
      @Override
      public <C, R> R handle(final C command, final Supplier<R> next) {

        for (int i = 0; i < 3; i++) {
          try {
            return next.get();
          } catch (final RuntimeException e) {
            if (i == 2) {
              throw e;
            }
          }
        }
        throw new IllegalStateException("unreachable");
      }
    };

    final PipelineBehavior txBehavior = new PipelineBehavior() {
      @Override
      public <C, R> R handle(final C command, final Supplier<R> next) {

        txOpenCount.add("tx-open");
        final var result = next.get();
        txOpenCount.add("tx-commit");
        return result;
      }
    };

    return new UseCasePipeline(List.of(retryBehavior, txBehavior));
  }

  @Test
  @DisplayName("múltiples behaviors se ejecutan en orden (primer behavior es el más externo)")
  void multipleBehaviorsExecuteInOrder() {

    final var log = new ArrayList<String>();

    final PipelineBehavior first = new PipelineBehavior() {
      @Override
      public <C, R> R handle(final C command, final Supplier<R> next) {

        log.add("first-before");
        final var result = next.get();
        log.add("first-after");
        return result;
      }
    };

    final PipelineBehavior second = new PipelineBehavior() {
      @Override
      public <C, R> R handle(final C command, final Supplier<R> next) {

        log.add("second-before");
        final var result = next.get();
        log.add("second-after");
        return result;
      }
    };

    final var pipeline = new UseCasePipeline(List.of(first, second));
    final UseCase<String, String> handler = command -> {
      log.add("handler");
      return "done";
    };

    pipeline.wrap(handler).handle("cmd");

    assertThat(log).containsExactly("first-before", "second-before", "handler", "second-after",
        "first-after");
  }

}
