package com.villo.truco.application.usecases.queries;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.queries.GetMatchStateQuery;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.exceptions.PlayerNotInMatchException;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.support.TestPublicActorResolver;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GetMatchStateQueryHandler")
class GetMatchStateQueryHandlerTest {

  private PlayerId playerOne;
  private PlayerId playerTwo;
  private Match match;
  private GetMatchStateQueryHandler handler;

  @BeforeEach
  void setUp() {

    this.playerOne = PlayerId.generate();
    this.playerTwo = PlayerId.generate();
    this.match = Match.createReady(playerOne, playerTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3)));

    final MatchQueryRepository queryRepo = new MatchQueryRepository() {

      @Override
      public Optional<Match> findById(final MatchId matchId) {

        return Optional.of(match);
      }

      @Override
      public Optional<Match> findByInviteCode(final InviteCode inviteCode) {

        return Optional.empty();
      }

      @Override
      public boolean hasActiveMatch(final PlayerId playerId) {

        return false;
      }

      @Override
      public boolean hasUnfinishedMatch(final PlayerId playerId) {

        return false;
      }

      @Override
      public List<MatchId> findIdleMatchIds(final Instant idleSince) {

        return List.of();
      }
    };

    this.handler = new GetMatchStateQueryHandler(queryRepo, TestPublicActorResolver.guestStyle());
  }

  @Test
  @DisplayName("lanza PlayerNotInMatchException cuando el jugador no pertenece a la partida")
  void throwsWhenPlayerNotInMatch() {

    final var outsider = PlayerId.generate();
    final var query = new GetMatchStateQuery(match.getId().value().toString(),
        outsider.value().toString());

    assertThatThrownBy(() -> handler.handle(query)).isInstanceOf(PlayerNotInMatchException.class);
  }

  @Test
  @DisplayName("devuelve estado correctamente cuando playerOne consulta")
  void succeedsForPlayerOne() {

    final var query = new GetMatchStateQuery(match.getId().value().toString(),
        playerOne.value().toString());

    assertThatNoException().isThrownBy(() -> handler.handle(query));
  }

  @Test
  @DisplayName("devuelve estado correctamente cuando playerTwo consulta")
  void succeedsForPlayerTwo() {

    final var query = new GetMatchStateQuery(match.getId().value().toString(),
        playerTwo.value().toString());

    assertThatNoException().isThrownBy(() -> handler.handle(query));
  }

}
