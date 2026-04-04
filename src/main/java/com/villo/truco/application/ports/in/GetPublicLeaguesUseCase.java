package com.villo.truco.application.ports.in;

import com.villo.truco.application.dto.PublicLeagueLobbyDTO;
import com.villo.truco.application.queries.GetPublicLeaguesQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;

public interface GetPublicLeaguesUseCase extends
    UseCase<GetPublicLeaguesQuery, CursorPageResult<PublicLeagueLobbyDTO>> {

}
