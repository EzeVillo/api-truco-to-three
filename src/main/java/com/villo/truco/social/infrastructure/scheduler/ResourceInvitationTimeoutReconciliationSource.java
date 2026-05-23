package com.villo.truco.social.infrastructure.scheduler;

import com.villo.truco.application.eventhandlers.TimeoutActionDispatcher;
import com.villo.truco.application.ports.out.timeout.EntityType;
import com.villo.truco.application.ports.out.timeout.TimeoutKey;
import com.villo.truco.application.ports.out.timeout.TimeoutReconciliationSource;
import com.villo.truco.social.domain.ports.ResourceInvitationRepository;
import java.util.stream.Stream;

public class ResourceInvitationTimeoutReconciliationSource implements TimeoutReconciliationSource {

  private final ResourceInvitationRepository repository;
  private final TimeoutActionDispatcher dispatcher;

  public ResourceInvitationTimeoutReconciliationSource(
      final ResourceInvitationRepository repository, final TimeoutActionDispatcher dispatcher) {

    this.repository = repository;
    this.dispatcher = dispatcher;
  }

  @Override
  public Stream<TimeoutEntry> activeWithDeadline() {

    return repository.findActiveWithExpiration().map(entry -> {
      final var key = TimeoutKey.of(EntityType.RESOURCE_INVITATION,
          entry.invitationId().value().toString());
      return new TimeoutEntry(key, entry.expiresAt(), dispatcher.buildAction(key));
    });
  }

}
