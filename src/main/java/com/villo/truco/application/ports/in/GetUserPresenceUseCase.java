package com.villo.truco.application.ports.in;

import com.villo.truco.application.dto.UserPresenceDTO;
import com.villo.truco.application.queries.GetUserPresenceQuery;

public interface GetUserPresenceUseCase extends UseCase<GetUserPresenceQuery, UserPresenceDTO> {

}
