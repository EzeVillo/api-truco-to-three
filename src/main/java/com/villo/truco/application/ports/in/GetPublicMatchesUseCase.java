package com.villo.truco.application.ports.in;

import com.villo.truco.application.dto.PublicMatchLobbyDTO;
import com.villo.truco.application.queries.GetPublicMatchesQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;

public interface GetPublicMatchesUseCase extends
    UseCase<GetPublicMatchesQuery, CursorPageResult<PublicMatchLobbyDTO>> {

}
