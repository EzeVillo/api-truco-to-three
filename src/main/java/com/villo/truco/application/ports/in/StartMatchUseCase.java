package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.StartMatchCommand;
import com.villo.truco.domain.shared.valueobjects.MatchId;

public interface StartMatchUseCase extends UseCase<StartMatchCommand, MatchId> {

}
