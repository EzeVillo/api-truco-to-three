package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.RespondTrucoCommand;
import com.villo.truco.domain.shared.valueobjects.MatchId;

public interface RespondTrucoUseCase extends UseCase<RespondTrucoCommand, MatchId> {

}
