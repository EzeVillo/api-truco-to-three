package com.villo.truco.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.application.dto.QuickMatchSearchDTO;
import com.villo.truco.application.dto.QuickMatchStatus;
import com.villo.truco.application.ports.in.CancelQuickMatchSearchUseCase;
import com.villo.truco.application.ports.in.EnqueueForQuickMatchUseCase;
import com.villo.truco.infrastructure.http.dto.request.QuickMatchRequest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayName("QuickMatchController")
class QuickMatchControllerTest {

  private EnqueueForQuickMatchUseCase enqueueUseCase;
  private CancelQuickMatchSearchUseCase cancelUseCase;
  private QuickMatchController controller;

  @BeforeEach
  void setUp() {

    enqueueUseCase = mock(EnqueueForQuickMatchUseCase.class);
    cancelUseCase = mock(CancelQuickMatchSearchUseCase.class);
    controller = new QuickMatchController(enqueueUseCase, cancelUseCase);
  }

  private Jwt jwt(final String subject) {

    return Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();
  }

  @Test
  @DisplayName("POST → SEARCHING returns 200 with SEARCHING status and null matchId")
  void postSearching() {

    final var enqueuedAt = Instant.now();
    when(enqueueUseCase.handle(any())).thenReturn(
        new QuickMatchSearchDTO(QuickMatchStatus.SEARCHING, null, enqueuedAt));

    final var response = controller.enqueue(new QuickMatchRequest(3),
        jwt("11111111-1111-1111-1111-111111111111"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo("SEARCHING");
    assertThat(response.getBody().matchId()).isNull();
    assertThat(response.getBody().enqueuedAt()).isEqualTo(enqueuedAt);
  }

  @Test
  @DisplayName("POST → MATCHED returns 200 with MATCHED status and matchId")
  void postMatched() {

    final var matchId = UUID.randomUUID();
    when(enqueueUseCase.handle(any())).thenReturn(
        new QuickMatchSearchDTO(QuickMatchStatus.MATCHED, matchId, Instant.now()));

    final var response = controller.enqueue(new QuickMatchRequest(3),
        jwt("22222222-2222-2222-2222-222222222222"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo("MATCHED");
    assertThat(response.getBody().matchId()).isEqualTo(matchId.toString());
  }

  @Test
  @DisplayName("DELETE returns 204 No Content")
  void deleteReturns204() {

    final var response = controller.cancel(jwt("33333333-3333-3333-3333-333333333333"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(cancelUseCase).handle(any());
  }

}
