package com.villo.truco.social.application.ports.in;

import com.villo.truco.application.ports.in.UseCase;
import com.villo.truco.social.application.commands.CancelResourceInvitationCommand;
import com.villo.truco.social.application.dto.ResourceInvitationDTO;

public interface CancelResourceInvitationUseCase extends
    UseCase<CancelResourceInvitationCommand, ResourceInvitationDTO> {

}
