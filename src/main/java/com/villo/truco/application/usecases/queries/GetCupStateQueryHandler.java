package com.villo.truco.application.usecases.queries;

import com.villo.truco.application.dto.CupBoutDTO;
import com.villo.truco.application.dto.CupRoundDTO;
import com.villo.truco.application.dto.CupStateDTO;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.application.ports.in.GetCupStateUseCase;
import com.villo.truco.application.queries.GetCupStateQuery;
import com.villo.truco.application.timeout.CupTimeoutPhasePolicy;
import com.villo.truco.application.timeout.TimeoutPhase;
import com.villo.truco.application.usecases.commands.CupResolver;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class GetCupStateQueryHandler implements GetCupStateUseCase {

  private final CupResolver cupResolver;
  private final PublicActorResolver publicActorResolver;
  private final long lobbyTimeoutMillis;
  private final CupTimeoutPhasePolicy phasePolicy = new CupTimeoutPhasePolicy();

  public GetCupStateQueryHandler(final CupResolver cupResolver,
      final PublicActorResolver publicActorResolver, final long lobbyTimeoutMillis) {

    this.cupResolver = Objects.requireNonNull(cupResolver);
    this.publicActorResolver = Objects.requireNonNull(publicActorResolver);
    this.lobbyTimeoutMillis = lobbyTimeoutMillis;
  }

  private static String roundName(final int roundNumber, final int totalRounds) {

    final int roundsFromFinal = totalRounds - roundNumber;
    return switch (roundsFromFinal) {
      case 0 -> "Final";
      case 1 -> "Semifinal";
      case 2 -> "Cuartos de final";
      default -> "Ronda " + roundNumber;
    };
  }

  private static String displayNameOf(final Map<PlayerId, String> actorNames,
      final PlayerId playerId) {

    return playerId != null ? actorNames.get(playerId) : null;
  }

  private Long lobbyDeadlineOf(final Cup cup) {

    if (this.phasePolicy.phaseOf(cup.getStatus()) != TimeoutPhase.LOBBY
        || cup.getLastActivityAt() == null) {
      return null;
    }
    return cup.getLastActivityAt().toEpochMilli() + this.lobbyTimeoutMillis;
  }

  @Override
  public CupStateDTO handle(final GetCupStateQuery query) {

    final var cup = this.cupResolver.resolve(query.cupId());

    cup.validatePlayerInCup(query.requestingPlayer());

    final var actorNames = this.publicActorResolver.resolveAll(Set.copyOf(cup.getParticipants()));
    final var champion = displayNameOf(actorNames, cup.getChampion());

    final var allRounds = cup.getRounds();
    final int totalRounds = allRounds.size();

    final var rounds = allRounds.stream().map(round -> {
      final var bouts = round.bouts().stream().map(
          bout -> new CupBoutDTO(bout.boutId().value().toString(), bout.roundNumber(),
              bout.bracketPosition(), displayNameOf(actorNames, bout.playerOne()),
              displayNameOf(actorNames, bout.playerTwo()),
              bout.matchId() != null ? bout.matchId().value().toString() : null,
              displayNameOf(actorNames, bout.winner()), bout.status().name())).toList();

      return new CupRoundDTO(round.roundNumber(), roundName(round.roundNumber(), totalRounds),
          bouts);
    }).toList();

    return new CupStateDTO(cup.getId().value().toString(), cup.getStatus().name(), rounds, champion,
        cup.getVisibility().name(), cup.getJoinCode().value(), this.lobbyDeadlineOf(cup));
  }

}
