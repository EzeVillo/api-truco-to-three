package com.villo.truco.infrastructure.persistence.repositories;

import com.villo.truco.domain.model.user.User;
import com.villo.truco.domain.ports.UserRepository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryUserRepository implements UserRepository {

  private final ConcurrentHashMap<String, User> store = new ConcurrentHashMap<>();

  @Override
  public void save(final User user) {

    this.store.put(user.username(), user);
  }

  @Override
  public Optional<User> findByUsername(final String username) {

    return Optional.ofNullable(this.store.get(username));
  }

  @Override
  public boolean existsByUsername(final String username) {

    return this.store.containsKey(username);
  }

}
