package com.villo.truco.social.infrastructure.persistence.repositories.spring;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.social.infrastructure.persistence.entities.FriendshipJpaEntity;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("SpringDataFriendshipRepository")
class SpringDataFriendshipRepositoryTest {

  @Autowired
  private SpringDataFriendshipRepository repository;

  private static FriendshipJpaEntity entity(final UUID requesterId, final UUID addresseeId,
      final String status) {

    final var entity = new FriendshipJpaEntity();
    entity.setId(UUID.randomUUID());
    entity.setRequesterId(requesterId);
    entity.setAddresseeId(addresseeId);
    entity.setStatus(status);
    return entity;
  }

  @Test
  @DisplayName("existsAcceptedByPlayers ignora historial no activo del mismo par")
  void existsAcceptedByPlayersIgnoresHistoricalNonActiveRows() {

    final var firstPlayerId = UUID.randomUUID();
    final var secondPlayerId = UUID.randomUUID();

    this.repository.save(entity(firstPlayerId, secondPlayerId, "DECLINED"));
    this.repository.save(entity(secondPlayerId, firstPlayerId, "CANCELLED"));
    this.repository.save(entity(firstPlayerId, secondPlayerId, "REMOVED"));
    this.repository.save(entity(secondPlayerId, firstPlayerId, "ACCEPTED"));

    final var accepted = this.repository.existsAcceptedByPlayers(firstPlayerId, secondPlayerId);

    assertThat(accepted).isTrue();
  }

  @Test
  @DisplayName("findPendingByRequesterAndAddressee respeta direccion del request pendiente")
  void findPendingByRequesterAndAddresseeRespectsDirection() {

    final var requesterId = UUID.randomUUID();
    final var addresseeId = UUID.randomUUID();

    this.repository.save(entity(requesterId, addresseeId, "PENDING"));
    this.repository.save(entity(addresseeId, requesterId, "DECLINED"));

    final var pending = this.repository.findPendingByRequesterAndAddressee(requesterId,
        addresseeId);
    final var reversed = this.repository.findPendingByRequesterAndAddressee(addresseeId,
        requesterId);

    assertThat(pending).isPresent();
    assertThat(reversed).isEmpty();
  }

  @Test
  @DisplayName("findAcceptedByPlayers devuelve solo la amistad aceptada entre ambos jugadores")
  void findAcceptedByPlayersReturnsOnlyAcceptedRelationship() {

    final var firstPlayerId = UUID.randomUUID();
    final var secondPlayerId = UUID.randomUUID();

    this.repository.save(entity(firstPlayerId, secondPlayerId, "PENDING"));
    this.repository.save(entity(secondPlayerId, firstPlayerId, "ACCEPTED"));

    final var accepted = this.repository.findAcceptedByPlayers(firstPlayerId, secondPlayerId);
    final var pending = this.repository.findPendingByPlayers(firstPlayerId, secondPlayerId);

    assertThat(accepted).isPresent();
    assertThat(accepted.get().getStatus()).isEqualTo("ACCEPTED");
    assertThat(pending).isPresent();
    assertThat(pending.get().getStatus()).isEqualTo("PENDING");
  }

}
