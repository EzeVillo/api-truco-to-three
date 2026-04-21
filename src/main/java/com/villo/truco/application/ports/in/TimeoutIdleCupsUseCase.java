package com.villo.truco.application.ports.in;

public interface TimeoutIdleCupsUseCase extends UseCase<Void, Void> {

  default void handle() {

    this.handle(null);
  }

}
