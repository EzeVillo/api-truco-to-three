package com.villo.truco.application.ports.in;

import com.villo.truco.application.commands.GuestLoginCommand;
import com.villo.truco.application.dto.GuestLoginDTO;

public interface GuestLoginUseCase extends UseCase<GuestLoginCommand, GuestLoginDTO> {

}
