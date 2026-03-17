package com.villo.truco.infrastructure.persistence.repositories;

import com.villo.truco.domain.model.user.User;
import com.villo.truco.domain.ports.UserRepository;
import com.villo.truco.infrastructure.persistence.mappers.UserMapper;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataUserRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaUserRepositoryAdapter implements UserRepository {

  private final SpringDataUserRepository springDataRepo;
  private final UserMapper mapper;

  public JpaUserRepositoryAdapter(final SpringDataUserRepository springDataRepo,
      final UserMapper mapper) {

    this.springDataRepo = springDataRepo;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public void save(final User user) {

    final var entity = this.mapper.toEntity(user);
    this.springDataRepo.saveAndFlush(entity);
  }

  @Override
  public Optional<User> findByUsername(final String username) {

    return this.springDataRepo.findByUsername(username).map(this.mapper::toDomain);
  }

  @Override
  public boolean existsByUsername(final String username) {

    return this.springDataRepo.existsByUsername(username);
  }

}
