package com.villo.truco.application.usecases.queries;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.queries.GetTournamentStateQuery;
import com.villo.truco.application.usecases.commands.TournamentResolver;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.tournament.Tournament;
import com.villo.truco.domain.model.tournament.exceptions.PlayerNotInTournamentException;
import com.villo.truco.domain.model.tournament.valueobjects.TournamentId;
import com.villo.truco.domain.ports.TournamentQueryRepository;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GetTournamentStateQueryHandler")
class GetTournamentStateQueryHandlerTest {

  private PlayerId creator;
  private PlayerId participant;
  private Tournament tournament;
  private GetTournamentStateQueryHandler handler;

  @BeforeEach
  void setUp() {

    this.creator = PlayerId.generate();
    this.participant = PlayerId.generate();
    this.tournament = Tournament.create(creator, 3, GamesToPlay.of(3));
    this.tournament.join(participant, tournament.getInviteCode());

    final TournamentQueryRepository queryRepo = new TournamentQueryRepository() {

      @Override
      public Optional<Tournament> findById(final TournamentId tournamentId) {

        return Optional.of(tournament);
      }

      @Override
      public Optional<Tournament> findByInviteCode(final InviteCode inviteCode) {

        return Optional.empty();
      }

      @Override
      public Optional<Tournament> findByMatchId(final MatchId matchId) {

        return Optional.empty();
      }
    };

    final var resolver = new TournamentResolver(queryRepo);
    this.handler = new GetTournamentStateQueryHandler(resolver);
  }

  @Test
  @DisplayName("lanza PlayerNotInTournamentException cuando el jugador no pertenece al torneo")
  void throwsWhenPlayerNotInTournament() {

    final var outsider = PlayerId.generate();
    final var query = new GetTournamentStateQuery(tournament.getId().value().toString(),
        outsider.value().toString());

    assertThatThrownBy(() -> handler.handle(query)).isInstanceOf(
        PlayerNotInTournamentException.class);
  }

  @Test
  @DisplayName("devuelve estado correctamente cuando el creador consulta")
  void succeedsForCreator() {

    final var query = new GetTournamentStateQuery(tournament.getId().value().toString(),
        creator.value().toString());

    assertThatNoException().isThrownBy(() -> handler.handle(query));
  }

  @Test
  @DisplayName("devuelve estado correctamente cuando un participante consulta")
  void succeedsForParticipant() {

    final var query = new GetTournamentStateQuery(tournament.getId().value().toString(),
        participant.value().toString());

    assertThatNoException().isThrownBy(() -> handler.handle(query));
  }

}
