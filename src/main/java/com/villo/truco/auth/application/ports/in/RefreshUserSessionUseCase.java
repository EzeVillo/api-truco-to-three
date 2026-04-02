package com.villo.truco.auth.application.ports.in;

import com.villo.truco.application.ports.in.UseCase;
import com.villo.truco.auth.application.commands.RefreshUserSessionCommand;
import com.villo.truco.auth.application.model.UserAuthenticatedSession;

public interface RefreshUserSessionUseCase extends
    UseCase<RefreshUserSessionCommand, UserAuthenticatedSession> {

}
