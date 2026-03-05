package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.PlayCardCommand;
import com.villo.truco.domain.model.match.valueobjects.MatchId;

public interface PlayCardUseCase {

  MatchId handle(PlayCardCommand command);

}
