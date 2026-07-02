package com.villo.truco.infrastructure.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import com.villo.truco.infrastructure.persistence.mappers.MatchMapper;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataMatchRepository;
import com.villo.truco.support.QueryCounter;
import jakarta.persistence.EntityManagerFactory;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Panel de amigos - conteo de queries del batch de partidas (N+1 baseline)")
class FriendPanelMatchQueryCountTest {

  private static final int PLAYER_COUNT = 8;

  @Autowired
  private SpringDataMatchRepository springDataMatchRepository;

  @Autowired
  private MatchMapper matchMapper;

  @Autowired
  private MatchQueryRepository matchQueryRepository;

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  @Test
  @DisplayName("findUnfinishedByPlayers resuelve N jugadores con consultas sublineales (sin N+1)")
  void findUnfinishedByPlayersDoesNotScaleQueriesWithPlayerCount() {

    final var players = new LinkedHashSet<PlayerId>();
    for (int i = 0; i < PLAYER_COUNT; i++) {
      final var player = PlayerId.generate();
      players.add(player);
      this.springDataMatchRepository.save(this.matchMapper.toEntity(
          Match.create(player, MatchRules.fromGamesToPlay(GamesToPlay.of(3), true),
              Visibility.PRIVATE)));
    }

    final var queryCounter = new QueryCounter(this.entityManagerFactory);
    final var counted = queryCounter.countReturning(
        () -> this.matchQueryRepository.findUnfinishedByPlayers(players));

    System.out.printf(
        "[query-count] findUnfinishedByPlayers con %d jugadores ejecuto %d sentencias "
            + "JDBC (techo: < %d)%n", PLAYER_COUNT, counted.queryCount(), PLAYER_COUNT);

    assertThat(counted.result()).hasSize(PLAYER_COUNT);
    assertThat(counted.queryCount()).isLessThan(PLAYER_COUNT);
  }

}
