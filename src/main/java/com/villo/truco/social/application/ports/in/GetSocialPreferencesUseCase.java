package com.villo.truco.social.application.ports.in;

import com.villo.truco.application.ports.in.UseCase;
import com.villo.truco.social.application.dto.SocialPreferencesDTO;
import com.villo.truco.social.application.queries.GetSocialPreferencesQuery;

public interface GetSocialPreferencesUseCase extends
    UseCase<GetSocialPreferencesQuery, SocialPreferencesDTO> {

}
