package com.villo.truco.application.commands;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record LeaveRematchCommand(MatchId originMatchId, PlayerId actor) {

  public LeaveRematchCommand {

    Objects.requireNonNull(originMatchId);
    Objects.requireNonNull(actor);
  }

  public LeaveRematchCommand(final String originMatchId, final String actor) {

    this(MatchId.of(originMatchId), PlayerId.of(actor));
  }

}
