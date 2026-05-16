package com.villo.truco.profile.application.usecases.queries;

import com.villo.truco.application.ports.in.UseCase;
import com.villo.truco.profile.application.dto.PlayerProfileDTO;

public interface GetPlayerProfileUseCase extends UseCase<GetPlayerProfileQuery, PlayerProfileDTO> {

}
