package com.villo.truco.application.ports.in;

public interface TimeoutIdleLeaguesUseCase extends UseCase<Void, Void> {

  default void handle() {

    this.handle(null);
  }

}
