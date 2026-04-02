package com.villo.truco.auth.infrastructure.persistence.repositories;

import com.villo.truco.auth.domain.model.user.User;
import com.villo.truco.auth.domain.model.user.UserRehydrator;
import com.villo.truco.auth.domain.model.user.exceptions.UsernameUnavailableException;
import com.villo.truco.auth.domain.model.user.valueobjects.Username;
import com.villo.truco.auth.domain.ports.UserRepository;
import com.villo.truco.auth.infrastructure.persistence.entities.UserJpaEntity;
import com.villo.truco.auth.infrastructure.persistence.mappers.UserMapper;
import com.villo.truco.auth.infrastructure.persistence.repositories.spring.SpringDataUserRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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
  public void saveEnsuringUsernameAvailable(final User user) {

    final var entity = this.mapper.toEntity(user);
    try {
      this.springDataRepo.saveAndFlush(entity);
    } catch (DataIntegrityViolationException ex) {
      if (this.isUsernameUniqueConstraintViolation(ex)) {
        throw new UsernameUnavailableException(user.username().value());
      }
      throw ex;
    }
  }

  @Override
  public Optional<User> findById(final PlayerId playerId) {

    return this.springDataRepo.findById(playerId.value()).map(this.mapper::toSnapshot)
        .map(UserRehydrator::rehydrate);
  }

  @Override
  public Optional<User> findByUsername(final Username username) {

    return this.springDataRepo.findByUsername(username.value()).map(this.mapper::toSnapshot)
        .map(UserRehydrator::rehydrate);
  }

  @Override
  public boolean existsByUsername(final Username username) {

    return this.springDataRepo.existsByUsername(username.value());
  }

  private boolean isUsernameUniqueConstraintViolation(final Throwable throwable) {

    var current = throwable;
    while (current != null) {
      if (current instanceof ConstraintViolationException cve
          && UserJpaEntity.USERNAME_UNIQUE_CONSTRAINT.equals(cve.getConstraintName())) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

}
