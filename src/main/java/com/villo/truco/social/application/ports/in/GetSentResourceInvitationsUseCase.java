package com.villo.truco.social.application.ports.in;

import com.villo.truco.application.ports.in.UseCase;
import com.villo.truco.social.application.dto.ResourceInvitationDTO;
import com.villo.truco.social.application.queries.GetSentResourceInvitationsQuery;
import java.util.List;

public interface GetSentResourceInvitationsUseCase extends
    UseCase<GetSentResourceInvitationsQuery, List<ResourceInvitationDTO>> {

}
