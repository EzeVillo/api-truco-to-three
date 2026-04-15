package com.villo.truco.infrastructure.persistence.repositories;

import com.villo.truco.domain.ports.JoinCodeRegistryQueryRepository;
import com.villo.truco.domain.ports.JoinCodeRegistryRepository;
import com.villo.truco.domain.shared.JoinCodeRegistration;
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

    final var entity = new JoinCodeRegistryJpaEntity();
    entity.setJoinCode(registration.joinCode().value());
    entity.setTargetType(registration.targetType().name());
    entity.setTargetId(registration.targetId());
    this.springDataRepository.save(entity);
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
