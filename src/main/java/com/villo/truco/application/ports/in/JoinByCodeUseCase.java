package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.JoinByCodeCommand;
import com.villo.truco.application.dto.JoinResourceDTO;

public interface JoinByCodeUseCase extends UseCase<JoinByCodeCommand, JoinResourceDTO> {

}
