package com.villo.truco.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataJoinCodeRegistryRepository;
import com.villo.truco.infrastructure.scheduler.TimeoutReconciliationRunner;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationId;
import com.villo.truco.social.domain.model.invitation.valueobjects.ResourceInvitationStatus;
import com.villo.truco.social.domain.ports.ResourceInvitationQueryRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Timeout exacto de invitación de recurso (integración)")
class TimeoutExactnessInvitationIT {

  @MockitoBean
  private SpringDataJoinCodeRegistryRepository springDataJoinCodeRegistryRepository;

  @Autowired
  private ResourceInvitationQueryRepository resourceInvitationQueryRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private TimeoutReconciliationRunner reconciliationRunner;

  @BeforeEach
  void setUp() {

    when(springDataJoinCodeRegistryRepository.insertIfAbsent(any(), any(), any())).thenReturn(1);
  }

  @Test
  @DisplayName("Invitación vencida es expirada dentro de ±1 s del vencimiento programado")
  void expiredInvitationTimesOutWithinTolerance() {

    final var invitationId = UUID.randomUUID();
    final var senderId = UUID.randomUUID();
    final var recipientId = UUID.randomUUID();
    final var targetId = UUID.randomUUID();
    final var pastExpiresAt = Timestamp.from(Instant.now().minusSeconds(3));

    jdbcTemplate.update("INSERT INTO social_resource_invitations "
            + "(id, sender_id, recipient_id, target_type, target_id, status, expires_at, version) "
            + "VALUES (?, ?, ?, 'MATCH', ?, 'PENDING', ?, 0)", invitationId, senderId, recipientId,
        targetId, pastExpiresAt);

    final var domainInvitationId = new ResourceInvitationId(invitationId);
    reconciliationRunner.reconcile("test");

    await().atMost(4, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS)
        .untilAsserted(() -> {
          final var invitation = resourceInvitationQueryRepository.findById(domainInvitationId);
          assertThat(invitation).isPresent();
          assertThat(invitation.get().getStatus()).isEqualTo(ResourceInvitationStatus.EXPIRED);
        });
  }

}
