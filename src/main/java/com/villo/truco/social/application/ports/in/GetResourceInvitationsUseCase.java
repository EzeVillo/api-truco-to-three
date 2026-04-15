package com.villo.truco.social.application.ports.in;

import com.villo.truco.application.ports.in.UseCase;
import com.villo.truco.social.application.dto.ResourceInvitationDTO;
import com.villo.truco.social.application.queries.GetResourceInvitationsQuery;
import java.util.List;

public interface GetResourceInvitationsUseCase extends
    UseCase<GetResourceInvitationsQuery, List<ResourceInvitationDTO>> {

}
