package com.villo.truco.application.ports.in;

import com.villo.truco.application.dto.MatchStateDTO;
import com.villo.truco.application.queries.GetMatchStateQuery;

public interface GetMatchStateUseCase {

    MatchStateDTO handle(GetMatchStateQuery query);

}
