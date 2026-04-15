package com.villo.truco.auth.domain.ports;

import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface UserQueryRepository {

  Map<PlayerId, String> findUsernamesByIds(Set<PlayerId> playerIds);

  Optional<PlayerId> findUserIdByUsername(String username);

}
