package com.villo.truco.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.domain.model.chat.Chat;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.model.league.events.LeagueStartedEvent;
import com.villo.truco.domain.model.league.valueobjects.LeagueId;
import com.villo.truco.domain.ports.ChatEventNotifier;
import com.villo.truco.domain.ports.ChatQueryRepository;
import com.villo.truco.domain.ports.ChatRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.repositories.InMemoryChatRepositoryAdapter;
import com.villo.truco.support.TestTransactionRunner;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChatLeagueStartedEventHandler")
class ChatLeagueStartedEventHandlerTest {

  private ChatRepository chatRepository;
  private ChatQueryRepository chatQueryRepository;
  private ChatEventNotifier chatEventNotifier;
  private ChatLeagueStartedEventHandler handler;

  @BeforeEach
  void setUp() {

    this.chatRepository = mock(ChatRepository.class);
    this.chatQueryRepository = mock(ChatQueryRepository.class);
    this.chatEventNotifier = mock(ChatEventNotifier.class);
    this.handler = new ChatLeagueStartedEventHandler(this.chatRepository, this.chatQueryRepository,
        this.chatEventNotifier);
  }

  @Test
  @DisplayName("eventType es LeagueStartedEvent")
  void eventTypeIsLeagueStartedEvent() {

    assertThat(this.handler.eventType()).isEqualTo(LeagueStartedEvent.class);
  }

  @Test
  @DisplayName("crea chat en LeagueStartedEvent")
  void createsOnLeagueStarted() {

    final var leagueId = LeagueId.generate();
    final var participants = List.of(PlayerId.generate(), PlayerId.generate(), PlayerId.generate());
    when(this.chatQueryRepository.findByParentTypeAndParentId(any(), any())).thenReturn(
        Optional.empty());

    this.handler.handle(new LeagueStartedEvent(leagueId, participants));

    verify(this.chatRepository).save(any(Chat.class));
    verify(this.chatEventNotifier).publishDomainEvents(any());
  }

  @Test
  @DisplayName("no crea chat duplicado")
  void doesNotCreateDuplicate() {

    final var leagueId = LeagueId.generate();
    final var existingChat = mock(Chat.class);
    when(this.chatQueryRepository.findByParentTypeAndParentId(eq(ChatParentType.LEAGUE),
        eq(leagueId.value().toString()))).thenReturn(Optional.of(existingChat));

    this.handler.handle(new LeagueStartedEvent(leagueId, List.of(PlayerId.generate())));

    verify(this.chatRepository, never()).save(any());
    verify(this.chatEventNotifier, never()).publishDomainEvents(any());
  }

  @Test
  @DisplayName("revierte la creacion si falla la publicacion")
  void rollsBackCreationWhenPublishingFails() {

    final var repository = new InMemoryChatRepositoryAdapter();
    final var leagueId = LeagueId.generate();
    final var participants = List.of(PlayerId.generate(), PlayerId.generate(), PlayerId.generate());
    final ChatEventNotifier failingNotifier = events -> {
      throw new IllegalStateException("ws failed");
    };
    final var realHandler = new ChatLeagueStartedEventHandler(repository, repository,
        failingNotifier);

    assertThatThrownBy(() -> TestTransactionRunner.inTransaction(
        () -> realHandler.handle(new LeagueStartedEvent(leagueId, participants)))).isInstanceOf(
        IllegalStateException.class).hasMessage("ws failed");

    assertThat(repository.findByParentTypeAndParentId(ChatParentType.LEAGUE,
        leagueId.value().toString())).isEmpty();
  }

}
