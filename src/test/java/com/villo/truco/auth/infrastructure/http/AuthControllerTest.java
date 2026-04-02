package com.villo.truco.auth.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.auth.application.model.GuestAuthenticatedSession;
import com.villo.truco.auth.application.model.UserAuthenticatedSession;
import com.villo.truco.auth.application.ports.in.GuestLoginUseCase;
import com.villo.truco.auth.application.ports.in.LoginUseCase;
import com.villo.truco.auth.application.ports.in.LogoutUserSessionUseCase;
import com.villo.truco.auth.application.ports.in.RefreshUserSessionUseCase;
import com.villo.truco.auth.application.ports.in.RegisterUserUseCase;
import com.villo.truco.auth.infrastructure.http.dto.request.LoginRequest;
import com.villo.truco.auth.infrastructure.http.dto.request.RefreshTokenRequest;
import com.villo.truco.auth.infrastructure.http.dto.request.RegisterUserRequest;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("AuthController")
class AuthControllerTest {

  @Test
  @DisplayName("register/login/guest/refresh/logout responden status esperado")
  void basicFlows() {

    final var register = mock(RegisterUserUseCase.class);
    final var login = mock(LoginUseCase.class);
    final var guest = mock(GuestLoginUseCase.class);
    final var refresh = mock(RefreshUserSessionUseCase.class);
    final var logout = mock(LogoutUserSessionUseCase.class);
    final var playerId = PlayerId.generate();

    when(register.handle(any())).thenReturn(
        new UserAuthenticatedSession(playerId, "jwt", 900, "refresh", 2592000));
    when(login.handle(any())).thenReturn(
        new UserAuthenticatedSession(playerId, "jwt", 900, "refresh", 2592000));
    when(guest.handle(any())).thenReturn(new GuestAuthenticatedSession(playerId, "jwt", 604800));
    when(refresh.handle(any())).thenReturn(
        new UserAuthenticatedSession(playerId, "jwt2", 900, "refresh2", 2592000));

    final var controller = new AuthController(register, login, guest, refresh, logout);

    assertThat(controller.register(new RegisterUserRequest("u", "p")).getStatusCode()).isEqualTo(
        HttpStatus.OK);
    assertThat(controller.login(new LoginRequest("u", "p")).getStatusCode()).isEqualTo(
        HttpStatus.OK);
    assertThat(controller.guest().getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(controller.refresh(new RefreshTokenRequest("refresh")).getStatusCode()).isEqualTo(
        HttpStatus.OK);
    assertThat(controller.logout(new RefreshTokenRequest("refresh")).getStatusCode()).isEqualTo(
        HttpStatus.NO_CONTENT);
  }

}
