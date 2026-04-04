package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.JoinPublicLeagueCommand;
import com.villo.truco.application.dto.JoinLeagueDTO;

public interface JoinPublicLeagueUseCase extends UseCase<JoinPublicLeagueCommand, JoinLeagueDTO> {

}
