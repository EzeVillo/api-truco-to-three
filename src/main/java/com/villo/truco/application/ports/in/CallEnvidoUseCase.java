package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.CallEnvidoCommand;
import com.villo.truco.domain.shared.valueobjects.MatchId;

public interface CallEnvidoUseCase extends UseCase<CallEnvidoCommand, MatchId> {

}
