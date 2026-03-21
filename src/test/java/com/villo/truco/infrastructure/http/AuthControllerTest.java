package com.villo.truco.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.dto.GuestLoginDTO;
import com.villo.truco.application.dto.LoginDTO;
import com.villo.truco.application.dto.RegisterUserDTO;
import com.villo.truco.application.ports.in.GuestLoginUseCase;
import com.villo.truco.application.ports.in.LoginUseCase;
import com.villo.truco.application.ports.in.RegisterUserUseCase;
import com.villo.truco.infrastructure.http.dto.request.LoginRequest;
import com.villo.truco.infrastructure.http.dto.request.RegisterUserRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("AuthController")
class AuthControllerTest {

  @Test
  @DisplayName("register/login/guest responden 200")
  void basicFlows() {

    final RegisterUserUseCase register = mock(RegisterUserUseCase.class);
    final LoginUseCase login = mock(LoginUseCase.class);
    final GuestLoginUseCase guest = mock(GuestLoginUseCase.class);

    when(register.handle(any())).thenReturn(new RegisterUserDTO("p1", "jwt"));
    when(login.handle(any())).thenReturn(new LoginDTO("p1", "jwt"));
    when(guest.handle(any())).thenReturn(new GuestLoginDTO("p1", "jwt"));

    final var controller = new AuthController(register, login, guest);

    assertThat(controller.register(new RegisterUserRequest("u", "p")).getStatusCode()).isEqualTo(
        HttpStatus.OK);
    assertThat(controller.login(new LoginRequest("u", "p")).getStatusCode()).isEqualTo(
        HttpStatus.OK);
    assertThat(controller.guest().getStatusCode()).isEqualTo(HttpStatus.OK);
  }

}
