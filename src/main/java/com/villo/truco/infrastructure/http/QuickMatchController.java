package com.villo.truco.infrastructure.http;

import com.villo.truco.application.commands.CancelQuickMatchSearchCommand;
import com.villo.truco.application.commands.EnqueueForQuickMatchCommand;
import com.villo.truco.application.dto.QuickMatchSearchDTO;
import com.villo.truco.application.ports.in.CancelQuickMatchSearchUseCase;
import com.villo.truco.application.ports.in.EnqueueForQuickMatchUseCase;
import com.villo.truco.infrastructure.http.dto.request.QuickMatchRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches/quick")
public class QuickMatchController {

  private static final Logger LOGGER = LoggerFactory.getLogger(QuickMatchController.class);

  private final EnqueueForQuickMatchUseCase enqueueForQuickMatch;
  private final CancelQuickMatchSearchUseCase cancelQuickMatchSearch;

  public QuickMatchController(final EnqueueForQuickMatchUseCase enqueueForQuickMatch,
      final CancelQuickMatchSearchUseCase cancelQuickMatchSearch) {

    this.enqueueForQuickMatch = Objects.requireNonNull(enqueueForQuickMatch);
    this.cancelQuickMatchSearch = Objects.requireNonNull(cancelQuickMatchSearch);
  }

  @PostMapping
  public ResponseEntity<QuickMatchSearchResponse> enqueue(
      @Valid @RequestBody final QuickMatchRequest request, @AuthenticationPrincipal final Jwt jwt) {

    LOGGER.info("HTTP quickMatch enqueue requested");
    final var dto = this.enqueueForQuickMatch.handle(
        new EnqueueForQuickMatchCommand(jwt.getSubject(), request.gamesToPlay(), null));
    return ResponseEntity.ok(QuickMatchSearchResponse.from(dto));
  }

  @DeleteMapping
  public ResponseEntity<Void> cancel(@AuthenticationPrincipal final Jwt jwt) {

    LOGGER.info("HTTP quickMatch cancel requested");
    this.cancelQuickMatchSearch.handle(new CancelQuickMatchSearchCommand(jwt.getSubject()));
    return ResponseEntity.noContent().build();
  }

  public record QuickMatchSearchResponse(String status, String matchId, Instant enqueuedAt) {

    static QuickMatchSearchResponse from(final QuickMatchSearchDTO dto) {

      return new QuickMatchSearchResponse(dto.status().name(),
          dto.matchId() != null ? dto.matchId().toString() : null, dto.enqueuedAt());
    }

  }

}
