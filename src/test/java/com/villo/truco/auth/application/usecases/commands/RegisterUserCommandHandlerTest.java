package com.villo.truco.auth.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.auth.application.commands.RegisterUserCommand;
import com.villo.truco.auth.application.ports.out.AccessTokenIssuer;
import com.villo.truco.auth.application.ports.out.RefreshTokenProvider;
import com.villo.truco.auth.application.services.UserSessionIssuer;
import com.villo.truco.auth.domain.model.auth.UserSession;
import com.villo.truco.auth.domain.model.user.UsernameAvailabilityPolicy;
import com.villo.truco.auth.domain.model.user.exceptions.InvalidPasswordException;
import com.villo.truco.auth.domain.model.user.exceptions.UsernameUnavailableException;
import com.villo.truco.auth.domain.model.user.valueobjects.HashedPassword;
import com.villo.truco.auth.domain.model.user.valueobjects.RawPassword;
import com.villo.truco.auth.domain.ports.AuthEventNotifier;
import com.villo.truco.auth.domain.ports.PasswordHasher;
import com.villo.truco.auth.domain.ports.UserRepository;
import com.villo.truco.auth.domain.ports.UserSessionRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("RegisterUserCommandHandler")
class RegisterUserCommandHandlerTest {

  private static AuthEventNotifier noopNotifier() {

    return events -> {
    };
  }

  @Test
  @DisplayName("registra un usuario nuevo y devuelve tokens")
  void registersNewUser() {

    final var savedUser = new AtomicReference<>();
    final var userRepository = mock(UserRepository.class);
    doAnswer(inv -> {
      savedUser.set(inv.getArgument(0));
      return null;
    }).when(userRepository).saveEnsuringUsernameAvailable(any());

    final var passwordHasher = mock(PasswordHasher.class);
    when(passwordHasher.hash(any(RawPassword.class))).thenAnswer(
        inv -> new HashedPassword("hashed:" + inv.getArgument(0, RawPassword.class).value()));

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

    final var userSessionRepository = mock(UserSessionRepository.class);
    final var issuer = new UserSessionIssuer(accessTokenIssuer, refreshTokenProvider,
        userSessionRepository, AuthTestFixtures.fixedClock());
    final var handler = new RegisterUserCommandHandler(userRepository, passwordHasher, issuer,
        new UsernameAvailabilityPolicy(userRepository), noopNotifier());

    final var session = handler.handle(new RegisterUserCommand("juancho", "pass1!"));

    assertThat(session.playerId()).isNotNull();
    assertThat(session.accessToken()).startsWith("access-");
    assertThat(session.refreshToken()).isEqualTo("refresh-value");
    assertThat(session.accessTokenExpiresIn()).isEqualTo(
        AuthTestFixtures.USER_ACCESS_TOKEN_EXPIRES_IN);
    assertThat(session.refreshTokenExpiresIn()).isEqualTo(
        AuthTestFixtures.REFRESH_TOKEN_EXPIRES_IN);
    assertThat(savedUser.get()).isNotNull();

    final var captor = ArgumentCaptor.forClass(UserSession.class);
    verify(userSessionRepository).save(captor.capture());
    assertThat(captor.getValue().containsRefreshTokenHash("hash-refresh-value")).isTrue();
  }

  @Test
  @DisplayName("falla si el username ya existe")
  void failsIfUsernameTaken() {

    final var userRepository = mock(UserRepository.class);
    when(userRepository.existsByUsername(any())).thenReturn(true);

    final var issuer = new UserSessionIssuer(mock(AccessTokenIssuer.class),
        mock(RefreshTokenProvider.class), mock(UserSessionRepository.class),
        AuthTestFixtures.fixedClock());
    final var handler = new RegisterUserCommandHandler(userRepository, mock(PasswordHasher.class),
        issuer, new UsernameAvailabilityPolicy(userRepository), noopNotifier());

    assertThatThrownBy(
        () -> handler.handle(new RegisterUserCommand("juancho", "other1!"))).isInstanceOf(
        UsernameUnavailableException.class);
  }

  @Test
  @DisplayName("falla si el username ya existe con diferente casing")
  void failsIfUsernameTakenWithDifferentCasing() {

    final var userRepository = mock(UserRepository.class);
    when(userRepository.existsByUsername(any())).thenReturn(true);

    final var issuer = new UserSessionIssuer(mock(AccessTokenIssuer.class),
        mock(RefreshTokenProvider.class), mock(UserSessionRepository.class),
        AuthTestFixtures.fixedClock());
    final var handler = new RegisterUserCommandHandler(userRepository, mock(PasswordHasher.class),
        issuer, new UsernameAvailabilityPolicy(userRepository), noopNotifier());

    assertThatThrownBy(
        () -> handler.handle(new RegisterUserCommand("JUANCHO", "other1!"))).isInstanceOf(
        UsernameUnavailableException.class);
  }

  @Test
  @DisplayName("falla si la password no cumple la politica de registro")
  void failsIfPasswordDoesNotMeetPolicy() {

    final var userRepository = mock(UserRepository.class);

    final var issuer = new UserSessionIssuer(mock(AccessTokenIssuer.class),
        mock(RefreshTokenProvider.class), mock(UserSessionRepository.class),
        AuthTestFixtures.fixedClock());
    final var handler = new RegisterUserCommandHandler(userRepository, mock(PasswordHasher.class),
        issuer, new UsernameAvailabilityPolicy(userRepository), noopNotifier());

    assertThatThrownBy(
        () -> handler.handle(new RegisterUserCommand("juancho", "abcde"))).isInstanceOf(
        InvalidPasswordException.class).hasMessage("Password must contain at least 1 number");
  }

}
