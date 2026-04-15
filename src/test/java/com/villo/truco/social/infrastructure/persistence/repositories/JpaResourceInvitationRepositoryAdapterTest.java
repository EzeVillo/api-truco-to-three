package com.villo.truco.social.infrastructure.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.domain.shared.exceptions.StaleAggregateException;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.invitation.ResourceInvitation;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;
import com.villo.truco.social.infrastructure.persistence.entities.ResourceInvitationJpaEntity;
import com.villo.truco.social.infrastructure.persistence.mappers.ResourceInvitationMapper;
import com.villo.truco.social.infrastructure.persistence.repositories.spring.SpringDataResourceInvitationRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@DisplayName("JpaResourceInvitationRepositoryAdapter")
class JpaResourceInvitationRepositoryAdapterTest {

  @Test
  @DisplayName("save actualiza version del agregado")
  void saveUpdatesVersion() {

    final var springRepo = mock(SpringDataResourceInvitationRepository.class);
    final var mapper = mock(ResourceInvitationMapper.class);
    final var adapter = new JpaResourceInvitationRepositoryAdapter(springRepo, mapper);
    final var invitation = ResourceInvitation.create(PlayerId.generate(), PlayerId.generate(),
        ResourceInvitationTargetType.MATCH, "11111111-1111-1111-1111-111111111111",
        Instant.parse("2026-04-04T12:00:00Z"), Duration.ofMinutes(10));
    final var entity = new ResourceInvitationJpaEntity();
    entity.setVersion(3);
    when(mapper.toEntity(invitation)).thenReturn(entity);

    adapter.save(invitation);

    verify(springRepo).saveAndFlush(entity);
    assertThat(invitation.getVersion()).isEqualTo(3);
  }

  @Test
  @DisplayName("save traduce optimistic lock")
  void saveTranslatesOptimisticLock() {

    final var springRepo = mock(SpringDataResourceInvitationRepository.class);
    final var mapper = mock(ResourceInvitationMapper.class);
    final var adapter = new JpaResourceInvitationRepositoryAdapter(springRepo, mapper);
    final var invitation = ResourceInvitation.create(PlayerId.generate(), PlayerId.generate(),
        ResourceInvitationTargetType.MATCH, "11111111-1111-1111-1111-111111111111",
        Instant.parse("2026-04-04T12:00:00Z"), Duration.ofMinutes(10));
    when(mapper.toEntity(invitation)).thenReturn(new ResourceInvitationJpaEntity());
    when(springRepo.saveAndFlush(any())).thenThrow(
        new ObjectOptimisticLockingFailureException("ResourceInvitation",
            invitation.getId().value()));

    assertThatThrownBy(() -> adapter.save(invitation)).isInstanceOf(StaleAggregateException.class);
  }

}
