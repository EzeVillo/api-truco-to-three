package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.PlayCardCommand;
import com.villo.truco.domain.shared.valueobjects.MatchId;

public interface PlayCardUseCase extends UseCase<PlayCardCommand, MatchId> {

}
