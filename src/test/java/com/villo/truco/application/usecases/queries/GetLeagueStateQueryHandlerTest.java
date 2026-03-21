package com.villo.truco.application.usecases.queries;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.queries.GetLeagueStateQuery;
import com.villo.truco.application.usecases.commands.LeagueResolver;
import com.villo.truco.domain.model.league.League;
import com.villo.truco.domain.model.league.exceptions.PlayerNotInLeagueException;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.ports.LeagueQueryRepository;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GetLeagueStateQueryHandler")
class GetLeagueStateQueryHandlerTest {

  private PlayerId creator;
  private PlayerId participant;
  private League league;
  private GetLeagueStateQueryHandler handler;

  @BeforeEach
  void setUp() {

    this.creator = PlayerId.generate();
    this.participant = PlayerId.generate();
    this.league = League.create(creator, 3, GamesToPlay.of(3));
    this.league.join(participant, league.getInviteCode());

    final LeagueQueryRepository queryRepo = new LeagueQueryRepository() {

      @Override
      public Optional<League> findById(final LeagueId leagueId) {

        return Optional.of(league);
      }

      @Override
      public Optional<League> findByInviteCode(final InviteCode inviteCode) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findByMatchId(final MatchId matchId) {

        return Optional.empty();
      }

      @Override
      public Optional<League> findInProgressByPlayer(final PlayerId playerId) {

        return Optional.empty();
      }
    };

    final var resolver = new LeagueResolver(queryRepo);
    this.handler = new GetLeagueStateQueryHandler(resolver);
  }

  @Test
  @DisplayName("lanza PlayerNotInLeagueException cuando el jugador no pertenece al liga")
  void throwsWhenPlayerNotInLeague() {

    final var outsider = PlayerId.generate();
    final var query = new GetLeagueStateQuery(league.getId().value().toString(),
        outsider.value().toString());

    assertThatThrownBy(() -> handler.handle(query)).isInstanceOf(PlayerNotInLeagueException.class);
  }

  @Test
  @DisplayName("devuelve estado correctamente cuando el creador consulta")
  void succeedsForCreator() {

    final var query = new GetLeagueStateQuery(league.getId().value().toString(),
        creator.value().toString());

    assertThatNoException().isThrownBy(() -> handler.handle(query));
  }

  @Test
  @DisplayName("devuelve estado correctamente cuando un participante consulta")
  void succeedsForParticipant() {

    final var query = new GetLeagueStateQuery(league.getId().value().toString(),
        participant.value().toString());

    assertThatNoException().isThrownBy(() -> handler.handle(query));
  }

}
