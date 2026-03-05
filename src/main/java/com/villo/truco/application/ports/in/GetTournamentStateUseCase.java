package com.villo.truco.application.ports.in;

import com.villo.truco.application.dto.TournamentStateDTO;
import com.villo.truco.application.queries.GetTournamentStateQuery;

public interface GetTournamentStateUseCase {

  TournamentStateDTO handle(GetTournamentStateQuery query);

}
