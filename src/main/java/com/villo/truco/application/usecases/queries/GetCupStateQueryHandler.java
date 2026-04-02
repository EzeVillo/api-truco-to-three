package com.villo.truco.application.usecases.queries;

import com.villo.truco.application.dto.CupBoutDTO;
import com.villo.truco.application.dto.CupRoundDTO;
import com.villo.truco.application.dto.CupStateDTO;
import com.villo.truco.application.ports.PublicActorResolver;
import com.villo.truco.application.ports.in.GetCupStateUseCase;
import com.villo.truco.application.queries.GetCupStateQuery;
import com.villo.truco.application.usecases.commands.CupResolver;
import java.util.Objects;

public final class GetCupStateQueryHandler implements GetCupStateUseCase {

  private final CupResolver cupResolver;
  private final PublicActorResolver publicActorResolver;

  public GetCupStateQueryHandler(final CupResolver cupResolver,
      final PublicActorResolver publicActorResolver) {

    this.cupResolver = Objects.requireNonNull(cupResolver);
    this.publicActorResolver = Objects.requireNonNull(publicActorResolver);
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

  @Override
  public CupStateDTO handle(final GetCupStateQuery query) {

    final var cup = this.cupResolver.resolve(query.cupId());

    cup.validatePlayerInCup(query.requestingPlayer());

    final var champion =
        cup.getChampion() != null ? this.publicActorResolver.resolve(cup.getChampion()) : null;

    final var allRounds = cup.getRounds();
    final int totalRounds = allRounds.size();

    final var rounds = allRounds.stream().map(round -> {
      final var bouts = round.bouts().stream().map(
          bout -> new CupBoutDTO(bout.boutId().value().toString(), bout.roundNumber(),
              bout.bracketPosition(),
              bout.playerOne() != null ? this.publicActorResolver.resolve(bout.playerOne()) : null,
              bout.playerTwo() != null ? this.publicActorResolver.resolve(bout.playerTwo()) : null,
              bout.matchId() != null ? bout.matchId().value().toString() : null,
              bout.winner() != null ? this.publicActorResolver.resolve(bout.winner()) : null,
              bout.status().name())).toList();

      return new CupRoundDTO(round.roundNumber(), roundName(round.roundNumber(), totalRounds),
          bouts);
    }).toList();

    return new CupStateDTO(cup.getId().value().toString(), cup.getStatus().name(), rounds,
        champion);
  }

}
