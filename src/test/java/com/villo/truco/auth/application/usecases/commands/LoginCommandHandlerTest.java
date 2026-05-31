package com.villo.truco.auth.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.auth.application.commands.LoginCommand;
import com.villo.truco.auth.application.exceptions.InvalidCredentialsException;
import com.villo.truco.auth.application.ports.out.AccessTokenIssuer;
import com.villo.truco.auth.application.ports.out.RefreshTokenProvider;
import com.villo.truco.auth.application.services.UserSessionIssuer;
import com.villo.truco.auth.domain.model.auth.UserSession;
import com.villo.truco.auth.domain.model.user.UserRehydrator;
import com.villo.truco.auth.domain.model.user.UserSnapshot;
import com.villo.truco.auth.domain.model.user.valueobjects.HashedPassword;
import com.villo.truco.auth.domain.model.user.valueobjects.RawPassword;
import com.villo.truco.auth.domain.model.user.valueobjects.Username;
import com.villo.truco.auth.domain.ports.PasswordHasher;
import com.villo.truco.auth.domain.ports.UserRepository;
import com.villo.truco.auth.domain.ports.UserSessionRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("LoginCommandHandler")
class LoginCommandHandlerTest {

  private static final PlayerId PLAYER_ID = PlayerId.generate();
  private static final String USERNAME = "juancho";
  private static final String RAW_PASSWORD = "secret";
  private static final String HASHED_PASSWORD = "hashed:secret";

  private PasswordHasher passwordHasher;
  private UserRepository userRepository;
  private UserSessionRepository userSessionRepository;

  @BeforeEach
  void setUp() {

    final var user = UserRehydrator.rehydrate(
        new UserSnapshot(PLAYER_ID, new Username(USERNAME), new HashedPassword(HASHED_PASSWORD)));

    passwordHasher = mock(PasswordHasher.class);
    when(passwordHasher.hash(any(RawPassword.class))).thenAnswer(
        inv -> new HashedPassword("hashed:" + inv.getArgument(0, RawPassword.class).value()));
    when(passwordHasher.matches(anyString(), any(HashedPassword.class))).thenAnswer(
        inv -> inv.getArgument(1, HashedPassword.class).value()
            .equals("hashed:" + inv.getArgument(0, String.class)));

    userRepository = mock(UserRepository.class);
    when(userRepository.findByUsername(new Username(USERNAME))).thenReturn(Optional.of(user));

    userSessionRepository = mock(UserSessionRepository.class);
  }

  @Test
  @DisplayName("login exitoso devuelve playerId, access token y refresh token")
  void loginSucceeds() {

    final var accessTokenIssuer = mock(AccessTokenIssuer.class);
    when(accessTokenIssuer.issueForUser(any())).thenAnswer(
        inv -> new AccessTokenIssuer.IssuedAccessToken(
            "access-" + inv.getArgument(0, PlayerId.class).value(),
            AuthTestFixtures.USER_ACCESS_TOKEN_EXPIRES_IN));

    final var refreshTokenProvider = mock(RefreshTokenProvider.class);
    when(refreshTokenProvider.hash(anyString())).thenAnswer(
        inv -> "hash-" + inv.getArgument(0, String.class));
    when(refreshTokenProvider.issue(any())).thenAnswer(
        inv -> new RefreshTokenProvider.IssuedRefreshToken("refresh-value", "hash-refresh-value",
            inv.getArgument(0, Instant.class)
                .plusSeconds(AuthTestFixtures.REFRESH_TOKEN_EXPIRES_IN),
            AuthTestFixtures.REFRESH_TOKEN_EXPIRES_IN));

    final var issuer = new UserSessionIssuer(accessTokenIssuer, refreshTokenProvider,
        this.userSessionRepository, AuthTestFixtures.fixedClock());
    final var handler = new LoginCommandHandler(this.userRepository, this.passwordHasher, issuer);

    final var session = handler.handle(new LoginCommand(USERNAME, RAW_PASSWORD));

    assertThat(session.playerId()).isEqualTo(PLAYER_ID);
    assertThat(session.username()).isEqualTo(USERNAME);
    assertThat(session.accessToken()).isEqualTo("access-" + PLAYER_ID.value());
    assertThat(session.refreshToken()).isEqualTo("refresh-value");
    assertThat(session.accessTokenExpiresIn()).isEqualTo(
        AuthTestFixtures.USER_ACCESS_TOKEN_EXPIRES_IN);
    assertThat(session.refreshTokenExpiresIn()).isEqualTo(
        AuthTestFixtures.REFRESH_TOKEN_EXPIRES_IN);

    final var captor = ArgumentCaptor.forClass(UserSession.class);
    verify(this.userSessionRepository).save(captor.capture());
    assertThat(captor.getValue().containsRefreshTokenHash("hash-refresh-value")).isTrue();
  }

  @Test
  @DisplayName("falla si el username no existe")
  void failsIfUsernameNotFound() {

    final var issuer = new UserSessionIssuer(mock(AccessTokenIssuer.class),
        mock(RefreshTokenProvider.class), this.userSessionRepository,
        AuthTestFixtures.fixedClock());
    final var handler = new LoginCommandHandler(this.userRepository, this.passwordHasher, issuer);

    assertThatThrownBy(
        () -> handler.handle(new LoginCommand("unknown", RAW_PASSWORD))).isInstanceOf(
        InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("falla si la contrasena es incorrecta")
  void failsIfWrongPassword() {

    final var issuer = new UserSessionIssuer(mock(AccessTokenIssuer.class),
        mock(RefreshTokenProvider.class), this.userSessionRepository,
        AuthTestFixtures.fixedClock());
    final var handler = new LoginCommandHandler(this.userRepository, this.passwordHasher, issuer);

    assertThatThrownBy(() -> handler.handle(new LoginCommand(USERNAME, "wrongpass"))).isInstanceOf(
        InvalidCredentialsException.class);
  }

}
