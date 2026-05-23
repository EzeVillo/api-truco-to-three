package com.villo.truco.social.domain.ports;

import com.villo.truco.social.domain.model.invitation.ResourceInvitation;
import java.util.stream.Stream;

public interface ResourceInvitationRepository {

  void save(ResourceInvitation invitation);

  Stream<ResourceInvitationTimeoutEntry> findActiveWithExpiration();

}
