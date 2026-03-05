package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.StartMatchCommand;
import com.villo.truco.domain.model.match.valueobjects.MatchId;

public interface StartMatchUseCase {

  MatchId handle(StartMatchCommand command);

}
