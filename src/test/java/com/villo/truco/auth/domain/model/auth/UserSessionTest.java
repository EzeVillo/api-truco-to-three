package com.villo.truco.auth.domain.model.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.auth.domain.model.auth.exceptions.InvalidUserSessionRefreshException;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserSession")
class UserSessionTest {

  private static final Instant NOW = Instant.parse("2026-03-30T21:00:00Z");

  @Test
  @DisplayName("rota el refresh token actual y agrega el reemplazo a la sesion")
  void rotatesCurrentRefreshToken() {

    final var session = UserSession.issue(PlayerId.generate(), "hash-refresh-0", NOW,
        NOW.plusSeconds(3600));

    final var replacement = session.rotate("hash-refresh-0", "hash-refresh-1", NOW,
        NOW.plusSeconds(7200));

    assertThat(session.refreshTokenSnapshots()).hasSize(2);
    assertThat(session.refreshTokenSnapshots().get(0).replacedByTokenId()).isEqualTo(
        replacement.getId());
    assertThat(session.refreshTokenSnapshots().get(1).tokenHash()).isEqualTo("hash-refresh-1");
  }

  @Test
  @DisplayName("revoca toda la sesion si se reutiliza un refresh token rotado")
  void revokesWholeSessionOnReplay() {

    final var session = UserSession.issue(PlayerId.generate(), "hash-refresh-0", NOW,
        NOW.plusSeconds(3600));
    session.rotate("hash-refresh-0", "hash-refresh-1", NOW, NOW.plusSeconds(7200));

    assertThatThrownBy(() -> session.rotate("hash-refresh-0", "hash-refresh-2", NOW.plusSeconds(10),
        NOW.plusSeconds(7210))).isInstanceOf(InvalidUserSessionRefreshException.class);

    assertThat(session.isRevoked()).isTrue();
    assertThat(session.refreshTokenSnapshots()).allMatch(token -> token.revokedAt() != null);
  }

}
