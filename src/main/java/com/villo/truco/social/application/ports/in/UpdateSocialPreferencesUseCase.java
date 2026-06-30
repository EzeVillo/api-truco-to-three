package com.villo.truco.social.application.ports.in;

import com.villo.truco.application.ports.in.UseCase;
import com.villo.truco.social.application.commands.UpdateSocialPreferencesCommand;
import com.villo.truco.social.application.dto.SocialPreferencesDTO;

public interface UpdateSocialPreferencesUseCase extends
    UseCase<UpdateSocialPreferencesCommand, SocialPreferencesDTO> {

}
