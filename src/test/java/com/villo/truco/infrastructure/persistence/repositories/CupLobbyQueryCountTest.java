package com.villo.truco.infrastructure.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.shared.pagination.CursorPageQuery;
import com.villo.truco.infrastructure.persistence.entities.CupJpaEntity;
import com.villo.truco.infrastructure.persistence.entities.CupParticipantJpaEntity;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataCupRepository;
import com.villo.truco.support.QueryCounter;
import jakarta.persistence.EntityManagerFactory;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Lobby de copas publicas - conteo de queries (sin N+1)")
class CupLobbyQueryCountTest {

  private static final int CUP_COUNT = 5;
  private static final long MAX_QUERIES = 4;

  @Autowired
  private SpringDataCupRepository springDataCupRepository;

  @Autowired
  private CupQueryRepository cupQueryRepository;

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  private static CupJpaEntity publicWaitingCup(final int index) {

    final var cup = new CupJpaEntity();
    cup.setId(UUID.randomUUID());
    cup.setNumberOfPlayers(4);
    cup.setGamesToPlay(3);
    cup.setJoinCode("CUP" + index);
    cup.setVisibility("PUBLIC");
    cup.setStatus("WAITING_FOR_PLAYERS");
    cup.addParticipant(new CupParticipantJpaEntity(cup, UUID.randomUUID(), 0));

    return cup;
  }

  @Test
  @DisplayName("findPublicWaiting carga N copas con un numero constante de queries (sin N+1)")
  void findPublicWaitingDoesNotScaleQueriesWithCupCount() {

    for (int i = 0; i < CUP_COUNT; i++) {
      this.springDataCupRepository.save(publicWaitingCup(i));
    }

    final var queryCounter = new QueryCounter(this.entityManagerFactory);
    final var counted = queryCounter.countReturning(
        () -> this.cupQueryRepository.findPublicWaiting(new CursorPageQuery(CUP_COUNT + 10, null)));

    System.out.printf("[query-count] findPublicWaiting con %d copas ejecuto %d sentencias JDBC "
        + "(techo: %d)%n", CUP_COUNT, counted.queryCount(), MAX_QUERIES);

    assertThat(counted.result().items()).hasSize(CUP_COUNT);
    // Con default_batch_fetch_size las 3 colecciones se cargan por lotes (IN), asi que el conteo
    // no crece con CUP_COUNT. Si esto se dispara, volvio el N+1.
    assertThat(counted.queryCount()).isLessThanOrEqualTo(MAX_QUERIES);
  }

}
