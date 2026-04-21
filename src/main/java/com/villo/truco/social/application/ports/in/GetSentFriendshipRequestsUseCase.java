package com.villo.truco.social.application.ports.in;

import com.villo.truco.application.ports.in.UseCase;
import com.villo.truco.social.application.dto.OutgoingFriendshipRequestDTO;
import com.villo.truco.social.application.queries.GetSentFriendshipRequestsQuery;
import java.util.List;

public interface GetSentFriendshipRequestsUseCase extends
    UseCase<GetSentFriendshipRequestsQuery, List<OutgoingFriendshipRequestDTO>> {

}
