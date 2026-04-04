package com.villo.truco.application.ports.in;

import com.villo.truco.application.dto.PublicCupLobbyDTO;
import com.villo.truco.application.queries.GetPublicCupsQuery;
import com.villo.truco.domain.shared.pagination.CursorPageResult;

public interface GetPublicCupsUseCase extends
    UseCase<GetPublicCupsQuery, CursorPageResult<PublicCupLobbyDTO>> {

}
