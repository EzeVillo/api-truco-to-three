package com.villo.truco.infrastructure.persistence.repositories;

import com.villo.truco.domain.ports.JoinCodeRegistryQueryRepository;
import com.villo.truco.domain.ports.JoinCodeRegistryRepository;
import com.villo.truco.domain.shared.JoinCodeRegistration;
import com.villo.truco.domain.shared.exceptions.JoinCodeRegistryCollisionException;
import com.villo.truco.domain.shared.valueobjects.JoinCode;
import com.villo.truco.domain.shared.valueobjects.JoinTargetType;
import com.villo.truco.infrastructure.persistence.entities.JoinCodeRegistryJpaEntity;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataJoinCodeRegistryRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JoinCodeRegistryJpaRepositoryAdapter implements JoinCodeRegistryRepository,
    JoinCodeRegistryQueryRepository {

  private final SpringDataJoinCodeRegistryRepository springDataRepository;

  public JoinCodeRegistryJpaRepositoryAdapter(
      final SpringDataJoinCodeRegistryRepository springDataRepository) {

    this.springDataRepository = Objects.requireNonNull(springDataRepository);
  }

  @Override
  @Transactional
  public void save(final JoinCodeRegistration registration) {

    final var inserted = this.springDataRepository.insertIfAbsent(registration.joinCode().value(),
        registration.targetType().name(), registration.targetId());
    if (inserted == 1) {
      return;
    }

    final var existing = this.findByJoinCode(registration.joinCode()).orElseThrow(
        () -> new IllegalStateException(
            "Join code registry rejected insert but row was not found for join code "
                + registration.joinCode().value()));

    if (existing.targetType() == registration.targetType() && existing.targetId()
        .equals(registration.targetId())) {
      return;
    }

    throw new JoinCodeRegistryCollisionException(registration.joinCode(), existing.targetType(),
        existing.targetId(), registration.targetType(), registration.targetId());
  }

  @Override
  public Optional<JoinCodeRegistration> findByJoinCode(final JoinCode joinCode) {

    return this.springDataRepository.findById(joinCode.value()).map(this::toDomain);
  }

  private JoinCodeRegistration toDomain(final JoinCodeRegistryJpaEntity entity) {

    return new JoinCodeRegistration(JoinCode.of(entity.getJoinCode()),
        JoinTargetType.valueOf(entity.getTargetType()), entity.getTargetId());
  }

}
