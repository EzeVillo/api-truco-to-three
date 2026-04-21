package com.villo.truco.social.application.ports.in;

import com.villo.truco.application.ports.in.UseCase;
import com.villo.truco.social.application.dto.IncomingFriendshipRequestDTO;
import com.villo.truco.social.application.queries.GetFriendshipRequestsQuery;
import java.util.List;

public interface GetFriendshipRequestsUseCase extends
    UseCase<GetFriendshipRequestsQuery, List<IncomingFriendshipRequestDTO>> {

}
