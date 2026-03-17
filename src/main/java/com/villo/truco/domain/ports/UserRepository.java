package com.villo.truco.domain.ports;

import com.villo.truco.domain.model.user.User;
import java.util.Optional;

public interface UserRepository {

  void save(User user);

  Optional<User> findByUsername(String username);

  boolean existsByUsername(String username);

}
