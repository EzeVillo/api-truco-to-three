package com.villo.truco.application.ports.in;

import com.villo.truco.application.dto.CupStateDTO;
import com.villo.truco.application.queries.GetCupStateQuery;

public interface GetCupStateUseCase extends UseCase<GetCupStateQuery, CupStateDTO> {

}
