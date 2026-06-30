package com.villo.truco.history.application.usecases.queries;

import com.villo.truco.application.ports.in.UseCase;
import com.villo.truco.history.application.dto.PlayerMatchHistoryDTO;

public interface GetPlayerMatchHistoryUseCase extends
    UseCase<GetPlayerMatchHistoryQuery, PlayerMatchHistoryDTO> {

}
