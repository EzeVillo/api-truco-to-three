package com.villo.truco.auth.domain.ports;

import com.villo.truco.auth.domain.model.auth.UserSession;
import com.villo.truco.auth.domain.model.auth.valueobjects.UserSessionId;
import java.util.Optional;

public interface UserSessionRepository {

  void save(UserSession session);

  Optional<UserSession> findById(UserSessionId id);

  Optional<UserSession> findByRefreshTokenHash(String tokenHash);

}
