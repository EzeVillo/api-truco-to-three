package com.villo.truco.testutil;

import com.villo.truco.social.domain.model.invitation.ResourceInvitation;
import com.villo.truco.social.domain.ports.ResourceInvitationRepository;
import com.villo.truco.social.domain.ports.ResourceInvitationTimeoutEntry;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class NoOpResourceInvitationRepository implements ResourceInvitationRepository {

  private final Consumer<ResourceInvitation> onSave;

  public NoOpResourceInvitationRepository(final Consumer<ResourceInvitation> onSave) {

    this.onSave = onSave;
  }

  @Override
  public void save(final ResourceInvitation invitation) {

    this.onSave.accept(invitation);
  }

  @Override
  public Stream<ResourceInvitationTimeoutEntry> findActiveWithExpiration() {

    return Stream.empty();
  }

}
