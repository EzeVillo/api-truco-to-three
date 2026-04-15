package com.villo.truco.social.infrastructure.persistence.repositories;

import com.villo.truco.domain.shared.exceptions.StaleAggregateException;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.social.domain.model.invitation.ResourceInvitation;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationId;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationTargetType;
import com.villo.truco.social.domain.ports.ResourceInvitationQueryRepository;
import com.villo.truco.social.domain.ports.ResourceInvitationRepository;
import com.villo.truco.social.infrastructure.persistence.mappers.ResourceInvitationMapper;
import com.villo.truco.social.infrastructure.persistence.repositories.spring.SpringDataResourceInvitationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaResourceInvitationRepositoryAdapter implements ResourceInvitationRepository,
    ResourceInvitationQueryRepository {

  private final SpringDataResourceInvitationRepository springDataResourceInvitationRepository;
  private final ResourceInvitationMapper resourceInvitationMapper;

  public JpaResourceInvitationRepositoryAdapter(
      final SpringDataResourceInvitationRepository springDataResourceInvitationRepository,
      final ResourceInvitationMapper resourceInvitationMapper) {

    this.springDataResourceInvitationRepository = springDataResourceInvitationRepository;
    this.resourceInvitationMapper = resourceInvitationMapper;
  }

  @Override
  @Transactional
  public void save(final ResourceInvitation invitation) {

    try {
      final var entity = this.resourceInvitationMapper.toEntity(invitation);
      this.springDataResourceInvitationRepository.saveAndFlush(entity);
      invitation.setVersion(entity.getVersion());
    } catch (final ObjectOptimisticLockingFailureException ex) {
      throw new StaleAggregateException(
          "Resource invitation " + invitation.getId() + " was modified concurrently", ex);
    }
  }

  @Override
  public Optional<ResourceInvitation> findById(final ResourceInvitationId invitationId) {

    return this.springDataResourceInvitationRepository.findById(invitationId.value())
        .map(this.resourceInvitationMapper::toDomain);
  }

  @Override
  public List<ResourceInvitation> findPendingReceivedBy(final PlayerId playerId) {

    return this.springDataResourceInvitationRepository.findByRecipientIdAndStatusOrderByExpiresAtAsc(
        playerId.value(), "PENDING").stream().map(this.resourceInvitationMapper::toDomain).toList();
  }

  @Override
  public List<ResourceInvitation> findPendingSentBy(final PlayerId playerId) {

    return this.springDataResourceInvitationRepository.findBySenderIdAndStatusOrderByExpiresAtAsc(
        playerId.value(), "PENDING").stream().map(this.resourceInvitationMapper::toDomain).toList();
  }

  @Override
  public List<ResourceInvitation> findPendingInvitations() {

    return this.springDataResourceInvitationRepository.findByStatusOrderByExpiresAtAsc("PENDING")
        .stream().map(this.resourceInvitationMapper::toDomain).toList();
  }

  @Override
  public boolean existsPendingBySenderAndRecipientAndTarget(final PlayerId senderId,
      final PlayerId recipientId, final ResourceInvitationTargetType targetType,
      final String targetId) {

    return this.springDataResourceInvitationRepository.existsBySenderIdAndRecipientIdAndTargetTypeAndTargetIdAndStatus(
        senderId.value(), recipientId.value(), targetType.name(), UUID.fromString(targetId),
        "PENDING");
  }

  @Override
  public List<ResourceInvitation> findPendingByTarget(final ResourceInvitationTargetType targetType,
      final String targetId) {

    return this.springDataResourceInvitationRepository.findByTargetTypeAndTargetIdAndStatus(
            targetType.name(), UUID.fromString(targetId), "PENDING").stream()
        .map(this.resourceInvitationMapper::toDomain).toList();
  }

}
