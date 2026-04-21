package com.villo.truco.application.ports.in;

public interface TimeoutIdleMatchesUseCase extends UseCase<Void, Void> {

  default void handle() {

    this.handle(null);
  }

}
