package com.villo.truco.application.commands;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Objects;

public record ChooseRematchCommand(MatchId originMatchId, PlayerId actor) {

  public ChooseRematchCommand {

    Objects.requireNonNull(originMatchId);
    Objects.requireNonNull(actor);
  }

  public ChooseRematchCommand(final String originMatchId, final String actor) {

    this(MatchId.of(originMatchId), PlayerId.of(actor));
  }

}
