package com.villo.truco.infrastructure.http;

import com.villo.truco.application.commands.ExchangeSessionGrantCommand;
import com.villo.truco.application.commands.RefreshSessionCommand;
import com.villo.truco.application.ports.in.ExchangeSessionGrantUseCase;
import com.villo.truco.application.ports.in.RefreshSessionUseCase;
import com.villo.truco.infrastructure.http.dto.request.ExchangeSessionGrantRequest;
import com.villo.truco.infrastructure.http.dto.request.RefreshSessionRequest;
import com.villo.truco.infrastructure.http.dto.response.SessionTokenResponse;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
public final class SessionController {

  private final ExchangeSessionGrantUseCase exchangeGrant;
  private final RefreshSessionUseCase refreshSession;

  public SessionController(final ExchangeSessionGrantUseCase exchangeGrant,
      final RefreshSessionUseCase refreshSession) {

    this.exchangeGrant = Objects.requireNonNull(exchangeGrant);
    this.refreshSession = Objects.requireNonNull(refreshSession);
  }

  @PostMapping("/exchange")
  public ResponseEntity<SessionTokenResponse> exchange(
      @RequestBody final ExchangeSessionGrantRequest request) {

    final var dto = this.exchangeGrant.handle(
        new ExchangeSessionGrantCommand(request.sessionGrant()));
    return ResponseEntity.ok(SessionTokenResponse.from(dto));
  }

  @PostMapping("/refresh")
  public ResponseEntity<SessionTokenResponse> refresh(
      @RequestBody final RefreshSessionRequest request) {

    final var dto = this.refreshSession.handle(new RefreshSessionCommand(request.refreshToken()));
    return ResponseEntity.ok(SessionTokenResponse.from(dto));
  }

}
