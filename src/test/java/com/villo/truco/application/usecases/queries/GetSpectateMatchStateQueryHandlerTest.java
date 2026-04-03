package com.villo.truco.application.usecases.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.assemblers.SpectatorMatchStateDTOAssembler;
import com.villo.truco.application.queries.GetSpectateMatchStateQuery;
import com.villo.truco.domain.model.match.Match;
import com.villo.truco.domain.model.match.valueobjects.MatchRules;
import com.villo.truco.domain.model.spectator.Spectatorship;
import com.villo.truco.domain.model.spectator.exceptions.NotSpectatingException;
import com.villo.truco.domain.ports.MatchQueryRepository;
import com.villo.truco.domain.shared.valueobjects.GamesToPlay;
import com.villo.truco.domain.shared.valueobjects.InviteCode;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.inmemory.InMemorySpectatorshipRepository;
import com.villo.truco.support.TestPublicActorResolver;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GetSpectateMatchStateQueryHandler")
class GetSpectateMatchStateQueryHandlerTest {

  private PlayerId spectator;
  private Match match;
  private InMemorySpectatorshipRepository repository;
  private GetSpectateMatchStateQueryHandler handler;

  @BeforeEach
  void setUp() {

    final var playerOne = PlayerId.generate();
    final var playerTwo = PlayerId.generate();
    this.spectator = PlayerId.generate();
    this.match = Match.createReady(playerOne, playerTwo,
        MatchRules.fromGamesToPlay(GamesToPlay.of(3)));
    this.match.startMatch(playerOne);
    this.match.startMatch(playerTwo);

    this.repository = new InMemorySpectatorshipRepository();

    final MatchQueryRepository matchRepo = new MatchQueryRepository() {

      @Override
      public Optional<Match> findById(final MatchId matchId) {

        return matchId.equals(match.getId()) ? Optional.of(match) : Optional.empty();
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

    final var assembler = new SpectatorMatchStateDTOAssembler(TestPublicActorResolver.guestStyle());
    this.handler = new GetSpectateMatchStateQueryHandler(matchRepo, this.repository, assembler);
  }

  @Test
  @DisplayName("devuelve estado cuando el espectador está registrado para el match")
  void success() {

    final var spectatorship = Spectatorship.create(this.spectator);
    spectatorship.startWatching(this.match.getId());
    this.repository.save(spectatorship);

    final var result = this.handler.handle(
        new GetSpectateMatchStateQuery(this.match.getId(), this.spectator));

    assertThat(result).isNotNull();
    assertThat(result.matchId()).isEqualTo(this.match.getId().value().toString());
    assertThat(result.spectatorCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("falla si el espectador no está registrado")
  void failsWhenNotSpectating() {

    assertThatThrownBy(() -> this.handler.handle(
        new GetSpectateMatchStateQuery(this.match.getId(), this.spectator))).isInstanceOf(
        NotSpectatingException.class);
  }

  @Test
  @DisplayName("falla si el espectador está en otro match")
  void failsWhenSpectatingDifferentMatch() {

    final var spectatorship = Spectatorship.create(this.spectator);
    spectatorship.startWatching(MatchId.generate());
    this.repository.save(spectatorship);

    assertThatThrownBy(() -> this.handler.handle(
        new GetSpectateMatchStateQuery(this.match.getId(), this.spectator))).isInstanceOf(
        NotSpectatingException.class);
  }

}
