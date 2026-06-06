package com.villo.truco.social.application.ports.in;

import com.villo.truco.social.application.dto.FriendActivityStateDTO;
import com.villo.truco.social.application.queries.GetFriendActivityQuery;

@FunctionalInterface
public interface GetFriendActivityUseCase {

  FriendActivityStateDTO handle(GetFriendActivityQuery query);

}
