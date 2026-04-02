package com.villo.truco.auth.infrastructure.http;

import com.villo.truco.auth.application.commands.GuestLoginCommand;
import com.villo.truco.auth.application.commands.LoginCommand;
import com.villo.truco.auth.application.commands.LogoutUserSessionCommand;
import com.villo.truco.auth.application.commands.RefreshUserSessionCommand;
import com.villo.truco.auth.application.commands.RegisterUserCommand;
import com.villo.truco.auth.application.ports.in.GuestLoginUseCase;
import com.villo.truco.auth.application.ports.in.LoginUseCase;
import com.villo.truco.auth.application.ports.in.LogoutUserSessionUseCase;
import com.villo.truco.auth.application.ports.in.RefreshUserSessionUseCase;
import com.villo.truco.auth.application.ports.in.RegisterUserUseCase;
import com.villo.truco.auth.infrastructure.http.dto.request.LoginRequest;
import com.villo.truco.auth.infrastructure.http.dto.request.RefreshTokenRequest;
import com.villo.truco.auth.infrastructure.http.dto.request.RegisterUserRequest;
import com.villo.truco.auth.infrastructure.http.dto.response.GuestLoginResponse;
import com.villo.truco.auth.infrastructure.http.dto.response.LoginResponse;
import com.villo.truco.auth.infrastructure.http.dto.response.RefreshUserSessionResponse;
import com.villo.truco.auth.infrastructure.http.dto.response.RegisterUserResponse;
import com.villo.truco.infrastructure.http.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticacion", description = "Endpoints para register, login, refresh, logout y guest")
public class AuthController {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

  private final RegisterUserUseCase registerUser;
  private final LoginUseCase login;
  private final GuestLoginUseCase guestLogin;
  private final RefreshUserSessionUseCase refreshUserSession;
  private final LogoutUserSessionUseCase logoutUserSession;

  public AuthController(final RegisterUserUseCase registerUser, final LoginUseCase login,
      final GuestLoginUseCase guestLogin, final RefreshUserSessionUseCase refreshUserSession,
      final LogoutUserSessionUseCase logoutUserSession) {

    this.registerUser = Objects.requireNonNull(registerUser);
    this.login = Objects.requireNonNull(login);
    this.guestLogin = Objects.requireNonNull(guestLogin);
    this.refreshUserSession = Objects.requireNonNull(refreshUserSession);
    this.logoutUserSession = Objects.requireNonNull(logoutUserSession);
  }

  @PostMapping("/register")
  @Operation(summary = "Registrar usuario", description = "Crea una cuenta con username y contraseña. Devuelve access token y refresh token.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Usuario registrado", content = @Content(schema = @Schema(implementation = RegisterUserResponse.class))),
      @ApiResponse(responseCode = "400", description = "Body invalido o faltante", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "422", description = "Username ya en uso", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<RegisterUserResponse> register(
      @Valid @RequestBody final RegisterUserRequest request) {

    LOGGER.info("HTTP register requested: username={}", request.username());
    final var session = this.registerUser.handle(
        new RegisterUserCommand(request.username(), request.password()));
    return ResponseEntity.ok(RegisterUserResponse.from(session));
  }

  @PostMapping("/login")
  @Operation(summary = "Login", description = "Autentica con username y contrasena. Devuelve access token y refresh token.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Login exitoso", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
      @ApiResponse(responseCode = "400", description = "Body invalido o faltante", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "Credenciales invalidas", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody final LoginRequest request) {

    LOGGER.info("HTTP login requested: username={}", request.username());
    final var session = this.login.handle(new LoginCommand(request.username(), request.password()));
    return ResponseEntity.ok(LoginResponse.from(session));
  }

  @PostMapping("/guest")
  @Operation(summary = "Acceso como invitado", description = "Genera un PlayerId efimero y devuelve solo un access token largo sin persistir cuenta.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Acceso como invitado concedido", content = @Content(schema = @Schema(implementation = GuestLoginResponse.class)))})
  public ResponseEntity<GuestLoginResponse> guest() {

    LOGGER.info("HTTP guest login requested");
    final var session = this.guestLogin.handle(new GuestLoginCommand());
    return ResponseEntity.ok(GuestLoginResponse.from(session));
  }

  @PostMapping("/refresh")
  @Operation(summary = "Refresh de sesion", description = "Rota el refresh token y devuelve un nuevo access token y refresh token.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Refresh exitoso", content = @Content(schema = @Schema(implementation = RefreshUserSessionResponse.class))),
      @ApiResponse(responseCode = "400", description = "Body invalido o faltante", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "Refresh token invalido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<RefreshUserSessionResponse> refresh(
      @Valid @RequestBody final RefreshTokenRequest request) {

    LOGGER.info("HTTP refresh requested");
    final var session = this.refreshUserSession.handle(
        new RefreshUserSessionCommand(request.refreshToken()));
    return ResponseEntity.ok(RefreshUserSessionResponse.from(session));
  }

  @DeleteMapping("/logout")
  @Operation(summary = "Cerrar sesion", description = "Revoca la sesion asociada al refresh token enviado.")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Sesion cerrada"),
      @ApiResponse(responseCode = "400", description = "Body invalido o faltante", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public ResponseEntity<Void> logout(@Valid @RequestBody final RefreshTokenRequest request) {

    LOGGER.info("HTTP logout requested");
    this.logoutUserSession.handle(new LogoutUserSessionCommand(request.refreshToken()));
    return ResponseEntity.noContent().build();
  }

}
