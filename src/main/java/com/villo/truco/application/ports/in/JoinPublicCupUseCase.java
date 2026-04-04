package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.JoinPublicCupCommand;
import com.villo.truco.application.dto.JoinCupDTO;

public interface JoinPublicCupUseCase extends UseCase<JoinPublicCupCommand, JoinCupDTO> {

}
