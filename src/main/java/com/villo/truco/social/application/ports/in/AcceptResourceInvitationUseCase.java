package com.villo.truco.social.application.ports.in;

import com.villo.truco.application.ports.in.UseCase;
import com.villo.truco.social.application.commands.AcceptResourceInvitationCommand;
import com.villo.truco.social.application.dto.ResourceInvitationDTO;

public interface AcceptResourceInvitationUseCase extends
    UseCase<AcceptResourceInvitationCommand, ResourceInvitationDTO> {

}
