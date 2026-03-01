package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.RespondEnvidoCommand;
import com.villo.truco.domain.model.match.valueobjects.EnvidoResult;
import java.util.Optional;

public interface RespondEnvidoUseCase {

    Optional<EnvidoResult> handle(RespondEnvidoCommand command);

}
