package com.villo.truco.application.ports.in;

import com.villo.truco.application.dto.LeagueStateDTO;
import com.villo.truco.application.queries.GetLeagueStateQuery;

public interface GetLeagueStateUseCase extends UseCase<GetLeagueStateQuery, LeagueStateDTO> {

}
