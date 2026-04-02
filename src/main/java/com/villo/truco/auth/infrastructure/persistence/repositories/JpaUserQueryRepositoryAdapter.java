package com.villo.truco.auth.infrastructure.persistence.repositories;

import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.auth.infrastructure.persistence.repositories.spring.SpringDataUserRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaUserQueryRepositoryAdapter implements UserQueryRepository {

  private final SpringDataUserRepository springDataUserRepository;

  public JpaUserQueryRepositoryAdapter(final SpringDataUserRepository springDataUserRepository) {

    this.springDataUserRepository = springDataUserRepository;
  }

  @Override
  public Map<PlayerId, String> findUsernamesByIds(final Set<PlayerId> playerIds) {

    if (playerIds.isEmpty()) {
      return Map.of();
    }

    final var usernamesById = new LinkedHashMap<PlayerId, String>();
    this.springDataUserRepository.findByIdIn(playerIds.stream().map(PlayerId::value).toList())
        .forEach(row -> usernamesById.put(new PlayerId(row.getId()), row.getUsername()));
    return usernamesById;
  }

}
