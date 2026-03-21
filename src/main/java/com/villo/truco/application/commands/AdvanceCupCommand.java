package com.villo.truco.application.commands;

import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record AdvanceCupCommand(CupId cupId, MatchId matchId, PlayerId winner) {

  public AdvanceCupCommand {

    Objects.requireNonNull(cupId);
    Objects.requireNonNull(matchId);
    Objects.requireNonNull(winner);
  }

}
