package com.villo.truco.infrastructure.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.infrastructure.persistence.entities.LeagueFixtureJpaEntity;
import com.villo.truco.infrastructure.persistence.entities.LeagueJpaEntity;
import com.villo.truco.infrastructure.persistence.entities.LeagueParticipantJpaEntity;
import com.villo.truco.infrastructure.persistence.entities.LeagueWinJpaEntity;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataLeagueRepository;
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
@DisplayName("Lobby de ligas publicas - conteo de queries (N+1 baseline)")
class LeagueLobbyQueryCountTest {

  private static final int LEAGUE_COUNT = 5;
  private static final long MAX_QUERIES = 4;
  @Autowired
  private SpringDataLeagueRepository springDataLeagueRepository;
  @Autowired
  private LeagueQueryRepository leagueQueryRepository;
  @Autowired
  private EntityManagerFactory entityManagerFactory;

  private static LeagueJpaEntity publicWaitingLeague(final int index) {

    final var league = new LeagueJpaEntity();
    league.setId(UUID.randomUUID());
    league.setNumberOfPlayers(4);
    league.setGamesToPlay(3);
    league.setJoinCode("LOBBY" + index);
    league.setVisibility("PUBLIC");
    league.setStatus("WAITING_FOR_PLAYERS");

    league.addParticipant(new LeagueParticipantJpaEntity(league, UUID.randomUUID(), 0));

    final var fixture = new LeagueFixtureJpaEntity();
    fixture.setId(UUID.randomUUID());
    fixture.setMatchdayNumber(1);
    fixture.setStatus("PENDING");
    league.addFixture(fixture);

    league.addWin(new LeagueWinJpaEntity(league, UUID.randomUUID(), 0));

    return league;
  }

  @Test
  @DisplayName("findPublicWaiting carga N ligas con un numero constante de queries (sin N+1)")
  void findPublicWaitingDoesNotScaleQueriesWithLeagueCount() {

    for (int i = 0; i < LEAGUE_COUNT; i++) {
      this.springDataLeagueRepository.save(publicWaitingLeague(i));
    }

    final var queryCounter = new QueryCounter(this.entityManagerFactory);
    final var counted = queryCounter.countReturning(this.leagueQueryRepository::findPublicWaiting);

    System.out.printf("[query-count] findPublicWaiting con %d ligas ejecuto %d sentencias JDBC "
        + "(techo: %d)%n", LEAGUE_COUNT, counted.queryCount(), MAX_QUERIES);

    assertThat(counted.result()).hasSize(LEAGUE_COUNT);
    assertThat(counted.queryCount()).isLessThanOrEqualTo(MAX_QUERIES);
  }

}
