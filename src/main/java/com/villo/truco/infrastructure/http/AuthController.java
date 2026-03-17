package com.villo.truco.infrastructure.http;

import com.villo.truco.application.commands.GuestLoginCommand;
import com.villo.truco.application.commands.LoginCommand;
import com.villo.truco.application.commands.RegisterUserCommand;
import com.villo.truco.application.ports.in.GuestLoginUseCase;
import com.villo.truco.application.ports.in.LoginUseCase;
import com.villo.truco.application.ports.in.RegisterUserUseCase;
import com.villo.truco.infrastructure.http.dto.request.GuestLoginRequest;
import com.villo.truco.infrastructure.http.dto.request.LoginRequest;
import com.villo.truco.infrastructure.http.dto.request.RegisterUserRequest;
import com.villo.truco.infrastructure.http.dto.response.ErrorResponse;
import com.villo.truco.infrastructure.http.dto.response.GuestLoginResponse;
import com.villo.truco.infrastructure.http.dto.response.LoginResponse;
import com.villo.truco.infrastructure.http.dto.response.RegisterUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Endpoints para registrar usuarios, hacer login y acceder como invitado")
public class AuthController {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

  private final RegisterUserUseCase registerUser;
  private final LoginUseCase login;
  private final GuestLoginUseCase guestLogin;

  public AuthController(final RegisterUserUseCase registerUser, final LoginUseCase login,
      final GuestLoginUseCase guestLogin) {

    this.registerUser = Objects.requireNonNull(registerUser);
    this.login = Objects.requireNonNull(login);
    this.guestLogin = Objects.requireNonNull(guestLogin);
  }

  @PostMapping("/register")
  @Operation(summary = "Registrar usuario", description = "Crea una cuenta con username y contraseña. Devuelve un JWT.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Usuario registrado", content = @Content(schema = @Schema(implementation = RegisterUserResponse.class))),
      @ApiResponse(responseCode = "422", description = "Username ya en uso", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<RegisterUserResponse> register(
      @RequestBody final RegisterUserRequest request) {

    LOGGER.info("HTTP register requested: username={}", request.username());
    final var dto = this.registerUser.handle(
        new RegisterUserCommand(request.username(), request.password()));
    return ResponseEntity.ok(RegisterUserResponse.from(dto));
  }

  @PostMapping("/login")
  @Operation(summary = "Login", description = "Autentica con username y contraseña. Devuelve un JWT.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Login exitoso", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
      @ApiResponse(responseCode = "401", description = "Credenciales inválidas", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<LoginResponse> login(@RequestBody final LoginRequest request) {

    LOGGER.info("HTTP login requested: username={}", request.username());
    final var dto = this.login.handle(new LoginCommand(request.username(), request.password()));
    return ResponseEntity.ok(LoginResponse.from(dto));
  }

  @PostMapping("/guest")
  @Operation(summary = "Acceso como invitado", description = "Genera un PlayerId efímero y devuelve un JWT sin persistir cuenta.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Acceso como invitado concedido", content = @Content(schema = @Schema(implementation = GuestLoginResponse.class)))})
  public ResponseEntity<GuestLoginResponse> guest(
      @RequestBody(required = false) final GuestLoginRequest request) {

    final var displayName = request != null ? request.displayName() : null;
    LOGGER.info("HTTP guest login requested: displayName={}", displayName);
    final var dto = this.guestLogin.handle(new GuestLoginCommand(displayName));
    return ResponseEntity.ok(GuestLoginResponse.from(dto));
  }

}
