package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.ports.RetryableTransactionalRunner;
import com.villo.truco.application.ports.in.ExpireRematchSessionUseCase;
import com.villo.truco.domain.model.rematch.RematchSession;
import com.villo.truco.domain.model.rematch.events.RematchSessionExpiredEvent;
import com.villo.truco.domain.model.rematch.valueobjects.RematchPlayerChoice;
import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionId;
import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionStatus;
import com.villo.truco.domain.ports.RematchSessionEventNotifier;
import com.villo.truco.domain.ports.RematchSessionRepository;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TimeoutIdempotencyTest — idempotencia al expirar entidades")
class TimeoutIdempotencyTest {

  private final RematchSessionId sessionId = RematchSessionId.generate();
  private RematchSessionRepository repository;
  private RematchSessionEventNotifier eventNotifier;
  private ExpireRematchSessionUseCase useCase;

  @BeforeEach
  void setUp() {

    repository = mock(RematchSessionRepository.class);
    eventNotifier = mock(RematchSessionEventNotifier.class);
    final Clock clock = Clock.systemUTC();

    final RetryableTransactionalRunner runner = Runnable::run;

    useCase = new ExpireRematchSessionCommandHandler(repository, eventNotifier, runner, clock);
  }

  @Test
  @DisplayName("Llamar handle(id) dos veces solo expira la sesión una vez (segunda llamada es no-op)")
  void handleTwiceOnlyExpiresSessionOnce() {

    final var openSession = RematchSession.reconstruct(sessionId, MatchId.generate(),
        PlayerId.generate(), PlayerId.generate(), false, false, 3, Instant.now().minusSeconds(60),
        RematchPlayerChoice.UNDECIDED, RematchPlayerChoice.UNDECIDED, RematchSessionStatus.OPEN,
        null);

    final var expiredSession = RematchSession.reconstruct(sessionId, MatchId.generate(),
        PlayerId.generate(), PlayerId.generate(), false, false, 3, Instant.now().minusSeconds(60),
        RematchPlayerChoice.UNDECIDED, RematchPlayerChoice.UNDECIDED, RematchSessionStatus.EXPIRED,
        null);

    when(repository.findById(sessionId)).thenReturn(Optional.of(openSession))
        .thenReturn(Optional.of(expiredSession));

    final List<List<?>> publishedBatches = new ArrayList<>();
    doAnswer(inv -> {
      publishedBatches.add(new ArrayList<>((List<?>) inv.getArgument(0)));
      return null;
    }).when(eventNotifier).publishDomainEvents(any());

    useCase.handle(sessionId);
    useCase.handle(sessionId);

    final var terminalEventCount = publishedBatches.stream().flatMap(List::stream)
        .filter(e -> e instanceof RematchSessionExpiredEvent).count();

    assertThat(terminalEventCount).isEqualTo(1);
  }

  @Test
  @DisplayName("expireIfNeeded sobre sesión ya expirada no genera eventos adicionales")
  void expireIfNeededIsIdempotentOnDomainModel() {

    final var session = RematchSession.reconstruct(sessionId, MatchId.generate(),
        PlayerId.generate(), PlayerId.generate(), false, false, 3, Instant.now().minusSeconds(60),
        RematchPlayerChoice.UNDECIDED, RematchPlayerChoice.UNDECIDED, RematchSessionStatus.OPEN,
        null);

    final var now = Instant.now();
    session.expireIfNeeded(now);
    session.expireIfNeeded(now);

    final var expiredEvents = session.getRematchDomainEvents().stream()
        .filter(e -> e instanceof RematchSessionExpiredEvent).count();

    assertThat(expiredEvents).isEqualTo(1);
    assertThat(session.getStatus()).isEqualTo(RematchSessionStatus.EXPIRED);
  }

}
