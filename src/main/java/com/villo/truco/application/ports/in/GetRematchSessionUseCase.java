package com.villo.truco.application.ports.in;

import com.villo.truco.application.dto.RematchSessionStateDTO;
import com.villo.truco.application.queries.GetRematchSessionQuery;

public interface GetRematchSessionUseCase extends
    UseCase<GetRematchSessionQuery, RematchSessionStateDTO> {

}
