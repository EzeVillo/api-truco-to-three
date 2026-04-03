package com.villo.truco.application.ports.in;

import com.villo.truco.application.dto.SpectatorMatchStateDTO;
import com.villo.truco.application.queries.GetSpectateMatchStateQuery;

public interface GetSpectateMatchStateUseCase extends
    UseCase<GetSpectateMatchStateQuery, SpectatorMatchStateDTO> {

}
