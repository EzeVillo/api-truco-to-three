package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.FoldCommand;
import com.villo.truco.domain.model.match.valueobjects.MatchId;

public interface FoldUseCase {

    MatchId handle(FoldCommand command);

}
