package com.villo.truco.auth.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.auth.application.model.AuthenticatedSessionIdentity;
import com.villo.truco.auth.application.model.GuestAuthenticatedSession;
import com.villo.truco.auth.application.model.UserAuthenticatedSession;
import com.villo.truco.auth.application.ports.in.GetCurrentSessionIdentityUseCase;
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
import org.springframework.security.oauth2.jwt.Jwt;

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
    final var me = mock(GetCurrentSessionIdentityUseCase.class);
    final var playerId = PlayerId.generate();

    when(register.handle(any())).thenReturn(
        new UserAuthenticatedSession(playerId, "juancho", "jwt", 900, "refresh", 2592000));
    when(login.handle(any())).thenReturn(
        new UserAuthenticatedSession(playerId, "juancho", "jwt", 900, "refresh", 2592000));
    when(guest.handle(any())).thenReturn(new GuestAuthenticatedSession(playerId, "jwt", 604800));
    when(refresh.handle(any())).thenReturn(
        new UserAuthenticatedSession(playerId, "juancho", "jwt2", 900, "refresh2", 2592000));
    when(me.handle(any())).thenReturn(
        new AuthenticatedSessionIdentity(playerId, "juancho", "user"));

    final var controller = new AuthController(register, login, guest, refresh, logout, me);

    final var registerResponse = controller.register(new RegisterUserRequest("u", "p"));
    assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(registerResponse.getBody().username()).isEqualTo("juancho");
    final var loginResponse = controller.login(new LoginRequest("u", "p"));
    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(loginResponse.getBody().username()).isEqualTo("juancho");
    assertThat(controller.guest().getStatusCode()).isEqualTo(HttpStatus.OK);
    final var refreshResponse = controller.refresh(new RefreshTokenRequest("refresh"));
    assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(refreshResponse.getBody().username()).isEqualTo("juancho");
    final var meResponse = controller.me(
        Jwt.withTokenValue("jwt").subject(playerId.value().toString()).claim("token_use", "user")
            .header("alg", "none").build());
    assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(meResponse.getBody().username()).isEqualTo("juancho");
    assertThat(meResponse.getBody().tokenUse()).isEqualTo("user");
    when(me.handle(any())).thenReturn(new AuthenticatedSessionIdentity(playerId, null, "guest"));
    final var guestMeResponse = controller.me(
        Jwt.withTokenValue("jwt").subject(playerId.value().toString()).claim("token_use", "guest")
            .header("alg", "none").build());
    assertThat(guestMeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(guestMeResponse.getBody().username()).isNull();
    assertThat(guestMeResponse.getBody().tokenUse()).isEqualTo("guest");
    assertThat(controller.logout(new RefreshTokenRequest("refresh")).getStatusCode()).isEqualTo(
        HttpStatus.NO_CONTENT);
  }

}
