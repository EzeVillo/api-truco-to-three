package com.villo.truco.social.domain.ports;

import com.villo.truco.social.domain.model.invitation.ResourceInvitation;

public interface ResourceInvitationRepository {

  void save(ResourceInvitation invitation);

}
