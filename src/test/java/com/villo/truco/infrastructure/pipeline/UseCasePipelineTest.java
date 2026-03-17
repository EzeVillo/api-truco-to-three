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
