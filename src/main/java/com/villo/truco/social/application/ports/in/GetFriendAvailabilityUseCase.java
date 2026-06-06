package com.villo.truco.social.application.ports.in;

import com.villo.truco.application.ports.in.UseCase;
import com.villo.truco.social.application.dto.FriendAvailabilityStateDTO;
import com.villo.truco.social.application.queries.GetFriendAvailabilityQuery;

public interface GetFriendAvailabilityUseCase extends
    UseCase<GetFriendAvailabilityQuery, FriendAvailabilityStateDTO> {

}
