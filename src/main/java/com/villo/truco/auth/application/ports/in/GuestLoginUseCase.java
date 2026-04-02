package com.villo.truco.auth.application.ports.in;

import com.villo.truco.application.ports.in.UseCase;
import com.villo.truco.auth.application.commands.GuestLoginCommand;
import com.villo.truco.auth.application.model.GuestAuthenticatedSession;

public interface GuestLoginUseCase extends UseCase<GuestLoginCommand, GuestAuthenticatedSession> {

}
