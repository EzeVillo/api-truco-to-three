package com.villo.truco.infrastructure.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.BotVsBotMatchRegistry;
import com.villo.truco.domain.ports.MatchRepository;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataJoinCodeRegistryRepository;
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
@DisplayName("JpaBotVsBotMatchRegistryAdapter")
class JpaBotVsBotMatchRegistryAdapterTest {

  @MockitoBean
  private SpringDataJoinCodeRegistryRepository springDataJoinCodeRegistryRepository;

  @Autowired
  private BotVsBotMatchRegistry registry;

  @Autowired
  private MatchRepository matchRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {

    when(this.springDataJoinCodeRegistryRepository.insertIfAbsent(any(), any(), any())).thenReturn(
        1);
  }

  private Match startedBotVsBotMatch() {

    final var botOne = PlayerId.generate();
    final var botTwo = PlayerId.generate();
    final var match = Match.createReady(botOne, botTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3), false));
    match.startMatch(botOne);
    match.startMatch(botTwo);
    this.matchRepository.save(match);
    return match;
  }

  @Test
  @DisplayName("register persiste la autoria y isBotVsBotMatch/findOwnerByMatchId la recuperan")
  void registerPersistsAuthorship() {

    final var matchId = MatchId.generate();
    final var ownerId = PlayerId.generate();

    this.registry.register(matchId, ownerId);

    assertThat(this.registry.isBotVsBotMatch(matchId)).isTrue();
    assertThat(this.registry.isBotVsBotMatch(MatchId.generate())).isFalse();
    assertThat(this.registry.findOwnerByMatchId(matchId)).contains(ownerId);
    assertThat(this.registry.findOwnerByMatchId(MatchId.generate())).isEmpty();
  }

  @Test
  @DisplayName("findActiveOwnedMatchId devuelve la partida del dueno mientras no este finalizada")
  void findActiveOwnedMatchIdReturnsUnfinishedMatch() {

    final var ownerId = PlayerId.generate();
    final var match = this.startedBotVsBotMatch();
    this.registry.register(match.getId(), ownerId);

    assertThat(this.registry.findActiveOwnedMatchId(ownerId)).contains(match.getId());
    assertThat(this.registry.findActiveOwnedMatchId(PlayerId.generate())).isEmpty();
  }

  @Test
  @DisplayName("findActiveOwnedMatchId queda vacio cuando la partida del dueno esta finalizada")
  void findActiveOwnedMatchIdEmptyWhenMatchFinished() {

    final var ownerId = PlayerId.generate();
    final var match = this.startedBotVsBotMatch();
    this.registry.register(match.getId(), ownerId);

    this.jdbcTemplate.update("UPDATE matches SET status = 'FINISHED' WHERE id = ?",
        match.getId().value());

    assertThat(this.registry.findActiveOwnedMatchId(ownerId)).isEmpty();
  }

}
