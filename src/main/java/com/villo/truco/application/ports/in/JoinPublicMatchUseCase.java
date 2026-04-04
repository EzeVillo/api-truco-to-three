package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.JoinPublicMatchCommand;
import com.villo.truco.application.dto.JoinMatchDTO;

public interface JoinPublicMatchUseCase extends UseCase<JoinPublicMatchCommand, JoinMatchDTO> {

}
