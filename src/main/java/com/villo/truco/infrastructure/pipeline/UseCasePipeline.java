package com.villo.truco.infrastructure.pipeline;

import com.villo.truco.application.ports.in.PipelineBehavior;
import com.villo.truco.application.ports.in.UseCase;
import java.util.List;
import java.util.function.Supplier;

public final class UseCasePipeline {

  private final List<PipelineBehavior> behaviors;

  public UseCasePipeline(final List<PipelineBehavior> behaviors) {

    this.behaviors = List.copyOf(behaviors);
  }

  public <C, R> UseCase<C, R> wrap(final UseCase<C, R> handler) {

    return command -> {
      Supplier<R> chain = () -> handler.handle(command);
      for (int i = behaviors.size() - 1; i >= 0; i--) {
        final var behavior = behaviors.get(i);
        final var next = chain;
        chain = () -> behavior.handle(command, next);
      }
      return chain.get();
    };
  }

}
