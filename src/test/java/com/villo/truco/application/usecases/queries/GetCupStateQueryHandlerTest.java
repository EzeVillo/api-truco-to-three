package com.villo.truco.application.usecases.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.queries.GetCupStateQuery;
import com.villo.truco.application.usecases.commands.CupResolver;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.exceptions.PlayerNotInCupException;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GetCupStateQueryHandler")
class GetCupStateQueryHandlerTest {

  @Test
  @DisplayName("devuelve estado cuando el jugador pertenece a la copa")
  void returnsStateWhenPlayerBelongs() {

    final var player = PlayerId.generate();
    final var p2 = PlayerId.generate();
    final var p3 = PlayerId.generate();
    final var p4 = PlayerId.generate();
    final var cup = Cup.create(player, 4, GamesToPlay.of(3));
    cup.join(p2, cup.getInviteCode());
    cup.join(p3, cup.getInviteCode());
    cup.join(p4, cup.getInviteCode());

    final var handler = new GetCupStateQueryHandler(new CupResolver(new CupQueryRepository() {
      @Override
      public Optional<Cup> findById(CupId cupId) {

        return java.util.Optional.of(cup);
      }

      @Override
      public Optional<Cup> findByInviteCode(InviteCode inviteCode) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findByMatchId(MatchId matchId) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findInProgressByPlayer(PlayerId playerId) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findWaitingByPlayer(PlayerId playerId) {

        return Optional.empty();
      }

      @Override
      public List<CupId> findIdleCupIds(java.time.Instant idleSince) {

        return List.of();
      }
    }));

    final var state = handler.handle(new GetCupStateQuery(cup.getId(), player));

    assertThat(state.cupId()).isEqualTo(cup.getId().value().toString());
    assertThat(state.status()).isEqualTo(cup.getStatus().name());
  }

  @Test
  @DisplayName("lanza PlayerNotInCupException si el solicitante no pertenece")
  void throwsWhenPlayerNotInCup() {

    final var owner = PlayerId.generate();
    final var stranger = PlayerId.generate();
    final var cup = Cup.create(owner, 4, GamesToPlay.of(3));

    final var handler = new GetCupStateQueryHandler(new CupResolver(new CupQueryRepository() {
      @Override
      public Optional<Cup> findById(CupId cupId) {

        return Optional.of(cup);
      }

      @Override
      public Optional<Cup> findByInviteCode(InviteCode inviteCode) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findByMatchId(MatchId matchId) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findInProgressByPlayer(PlayerId playerId) {

        return Optional.empty();
      }

      @Override
      public Optional<Cup> findWaitingByPlayer(PlayerId playerId) {

        return Optional.empty();
      }

      @Override
      public List<CupId> findIdleCupIds(Instant idleSince) {

        return List.of();
      }
    }));

    assertThatThrownBy(
        () -> handler.handle(new GetCupStateQuery(cup.getId(), stranger))).isInstanceOf(
        PlayerNotInCupException.class);
  }

}
