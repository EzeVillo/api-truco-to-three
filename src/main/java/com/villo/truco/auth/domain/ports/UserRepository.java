package com.villo.truco.auth.domain.ports;

import com.villo.truco.auth.domain.model.user.User;
import com.villo.truco.auth.domain.model.user.valueobjects.Username;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;

public interface UserRepository {

  void saveEnsuringUsernameAvailable(User user);

  Optional<User> findById(PlayerId playerId);

  Optional<User> findByUsername(Username username);

  boolean existsByUsername(Username username);

}
