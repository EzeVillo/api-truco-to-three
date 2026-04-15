package com.villo.truco.social.application.ports.in;

import com.villo.truco.application.ports.in.UseCase;

public interface ExpirePendingResourceInvitationsUseCase extends UseCase<Void, Void> {

  default void handle() {

    this.handle(null);
  }

}
