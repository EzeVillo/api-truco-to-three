package com.villo.truco.social.application.ports.in;

import com.villo.truco.application.ports.in.UseCase;
import com.villo.truco.social.application.dto.FriendSummaryDTO;
import com.villo.truco.social.application.queries.GetFriendsQuery;
import java.util.List;

public interface GetFriendsUseCase extends UseCase<GetFriendsQuery, List<FriendSummaryDTO>> {

}
