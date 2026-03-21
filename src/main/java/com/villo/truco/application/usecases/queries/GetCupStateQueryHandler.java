package com.villo.truco.application.usecases.queries;

import com.villo.truco.application.dto.CupBoutDTO;
import com.villo.truco.application.dto.CupRoundDTO;
import com.villo.truco.application.dto.CupStateDTO;
import com.villo.truco.application.ports.in.GetCupStateUseCase;
import com.villo.truco.application.queries.GetCupStateQuery;
import com.villo.truco.application.usecases.commands.CupResolver;
import com.villo.truco.domain.model.cup.exceptions.PlayerNotInCupException;
import java.util.Objects;

public final class GetCupStateQueryHandler implements GetCupStateUseCase {

  private final CupResolver cupResolver;

  public GetCupStateQueryHandler(final CupResolver cupResolver) {

    this.cupResolver = Objects.requireNonNull(cupResolver);
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

    if (!cup.hasPlayer(query.requestingPlayer())) {
      throw new PlayerNotInCupException();
    }

    final var champion = cup.getChampion() != null ? cup.getChampion().value().toString() : null;

    final var allRounds = cup.getRounds();
    final int totalRounds = allRounds.size();

    final var rounds = allRounds.stream().map(round -> {
      final var bouts = round.bouts().stream().map(
          bout -> new CupBoutDTO(bout.boutId().value().toString(), bout.roundNumber(),
              bout.bracketPosition(),
              bout.playerOne() != null ? bout.playerOne().value().toString() : null,
              bout.playerTwo() != null ? bout.playerTwo().value().toString() : null,
              bout.matchId() != null ? bout.matchId().value().toString() : null,
              bout.winner() != null ? bout.winner().value().toString() : null,
              bout.status().name())).toList();

      return new CupRoundDTO(round.roundNumber(), roundName(round.roundNumber(), totalRounds),
          bouts);
    }).toList();

    return new CupStateDTO(cup.getId().value().toString(), cup.getStatus().name(), rounds,
        champion);
  }

}
