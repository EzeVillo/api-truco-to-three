package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.CallTrucoCommand;
import com.villo.truco.domain.model.match.valueobjects.MatchId;

public interface CallTrucoUseCase extends UseCase<CallTrucoCommand, MatchId> {

}
