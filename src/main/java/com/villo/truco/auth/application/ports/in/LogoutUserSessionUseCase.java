package com.villo.truco.auth.application.ports.in;

import com.villo.truco.application.ports.in.UseCase;
import com.villo.truco.auth.application.commands.LogoutUserSessionCommand;

public interface LogoutUserSessionUseCase extends UseCase<LogoutUserSessionCommand, Void> {

}
