package com.villo.truco.domain.model.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.domain.model.chat.events.ChatCreatedEvent;
import com.villo.truco.domain.model.chat.events.MessageSentEvent;
import com.villo.truco.domain.model.chat.exceptions.ChatMessageEmptyException;
import com.villo.truco.domain.model.chat.exceptions.ChatMessageTooLongException;
import com.villo.truco.domain.model.chat.exceptions.ChatRateLimitExceededException;
import com.villo.truco.domain.model.chat.exceptions.PlayerNotInChatException;
import com.villo.truco.domain.model.chat.valueobjects.ChatParentType;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ChatTest {

    private PlayerId playerOne;
    private PlayerId playerTwo;

    @BeforeEach
    void setUp() {

        this.playerOne = PlayerId.generate();
        this.playerTwo = PlayerId.generate();
    }

    private Chat createChat() {

        return Chat.create(ChatParentType.MATCH, UUID.randomUUID().toString(),
            Set.of(this.playerOne, this.playerTwo), Duration.ZERO);
    }

    private Chat createChatWithRateLimit() {

        return Chat.create(ChatParentType.MATCH, UUID.randomUUID().toString(),
            Set.of(this.playerOne, this.playerTwo), Duration.ofSeconds(10));
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create chat with participants and emit ChatCreatedEvent")
        void createChat_emitsChatCreatedEvent() {

            final var chat = createChat();

            assertThat(chat.getParticipants()).containsExactlyInAnyOrder(playerOne, playerTwo);
            assertThat(chat.getParentType()).isEqualTo(ChatParentType.MATCH);
            assertThat(chat.getMessages()).isEmpty();
            assertThat(chat.getDomainEvents()).hasSize(1);
            assertThat(chat.getDomainEvents().getFirst()).isInstanceOf(ChatCreatedEvent.class);
        }

        @Test
        @DisplayName("should reject creation with less than 2 participants")
        void createChat_lessThanTwoParticipants_throws() {

            assertThatThrownBy(
                () -> Chat.create(ChatParentType.MATCH, "id", Set.of(playerOne), Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 2 participants");
        }

    }

    @Nested
    @DisplayName("sendMessage")
    class SendMessage {

        @Test
        @DisplayName("should add message and emit MessageSentEvent")
        void sendMessage_validParticipant_addsMessage() {

            final var chat = createChat();
            chat.clearDomainEvents();

            chat.sendMessage(playerOne, "Hello");

            assertThat(chat.getMessages()).hasSize(1);
            assertThat(chat.getMessages().getFirst().getContent()).isEqualTo("Hello");
            assertThat(chat.getMessages().getFirst().getSenderId()).isEqualTo(playerOne);
            assertThat(chat.getDomainEvents()).hasSize(1);
            assertThat(chat.getDomainEvents().getFirst()).isInstanceOf(MessageSentEvent.class);
        }

        @Test
        @DisplayName("should reject message from non-participant")
        void sendMessage_nonParticipant_throws() {

            final var chat = createChat();
            final var stranger = PlayerId.generate();

            assertThatThrownBy(() -> chat.sendMessage(stranger, "Hi"))
                .isInstanceOf(PlayerNotInChatException.class);
        }

        @Test
        @DisplayName("should reject empty message")
        void sendMessage_emptyContent_throws() {

            final var chat = createChat();

            assertThatThrownBy(() -> chat.sendMessage(playerOne, ""))
                .isInstanceOf(ChatMessageEmptyException.class);
        }

        @Test
        @DisplayName("should reject blank message")
        void sendMessage_blankContent_throws() {

            final var chat = createChat();

            assertThatThrownBy(() -> chat.sendMessage(playerOne, "   "))
                .isInstanceOf(ChatMessageEmptyException.class);
        }

        @Test
        @DisplayName("should reject null message")
        void sendMessage_nullContent_throws() {

            final var chat = createChat();

            assertThatThrownBy(() -> chat.sendMessage(playerOne, null))
                .isInstanceOf(ChatMessageEmptyException.class);
        }

        @Test
        @DisplayName("should reject message exceeding max length")
        void sendMessage_tooLong_throws() {

            final var chat = createChat();
            final var longContent = "x".repeat(501);

            assertThatThrownBy(() -> chat.sendMessage(playerOne, longContent))
                .isInstanceOf(ChatMessageTooLongException.class);
        }

        @Test
        @DisplayName("should accept message at exactly max length")
        void sendMessage_exactMaxLength_succeeds() {

            final var chat = createChat();
            final var content = "x".repeat(500);

            chat.sendMessage(playerOne, content);

            assertThat(chat.getMessages()).hasSize(1);
        }

    }

    @Nested
    @DisplayName("rateLimit")
    class RateLimit {

        @Test
        @DisplayName("should reject message when rate limited")
        void sendMessage_rateLimited_throws() {

            final var chat = createChatWithRateLimit();
            chat.sendMessage(playerOne, "first");

            assertThatThrownBy(() -> chat.sendMessage(playerOne, "second"))
                .isInstanceOf(ChatRateLimitExceededException.class);
        }

        @Test
        @DisplayName("should allow different players to send consecutively")
        void sendMessage_differentPlayers_allowed() {

            final var chat = createChatWithRateLimit();

            chat.sendMessage(playerOne, "from player one");
            chat.sendMessage(playerTwo, "from player two");

            assertThat(chat.getMessages()).hasSize(2);
        }

        @Test
        @DisplayName("should allow rapid messages with zero cooldown")
        void sendMessage_zeroCooldown_allowed() {

            final var chat = createChat();

            chat.sendMessage(playerOne, "first");
            chat.sendMessage(playerOne, "second");
            chat.sendMessage(playerOne, "third");

            assertThat(chat.getMessages()).hasSize(3);
        }

    }

    @Nested
    @DisplayName("circularBuffer")
    class CircularBuffer {

        @Test
        @DisplayName("should evict oldest message when buffer is full")
        void sendMessage_exceedsMaxMessages_evictsOldest() {

            final var chat = createChat();

            for (int i = 0; i < Chat.MAX_MESSAGES; i++) {
                chat.sendMessage(playerOne, "msg-" + i);
            }

            assertThat(chat.getMessages()).hasSize(Chat.MAX_MESSAGES);
            assertThat(chat.getMessages().getFirst().getContent()).isEqualTo("msg-0");

            chat.sendMessage(playerOne, "overflow");

            assertThat(chat.getMessages()).hasSize(Chat.MAX_MESSAGES);
            assertThat(chat.getMessages().getFirst().getContent()).isEqualTo("msg-1");
            assertThat(chat.getMessages().get(Chat.MAX_MESSAGES - 1).getContent()).isEqualTo(
                "overflow");
        }

        @Test
        @DisplayName("should handle multiple evictions correctly")
        void sendMessage_multipleEvictions_maintainsOrder() {

            final var chat = createChat();

            for (int i = 0; i < Chat.MAX_MESSAGES + 10; i++) {
                chat.sendMessage(playerOne, "msg-" + i);
            }

            assertThat(chat.getMessages()).hasSize(Chat.MAX_MESSAGES);
            assertThat(chat.getMessages().getFirst().getContent()).isEqualTo("msg-10");
            assertThat(chat.getMessages().get(Chat.MAX_MESSAGES - 1).getContent())
                .isEqualTo("msg-" + (Chat.MAX_MESSAGES + 9));
        }

    }

    @Nested
    @DisplayName("validateParticipant")
    class ValidateParticipant {

        @Test
        @DisplayName("should succeed for valid participant")
        void validateParticipant_valid_succeeds() {

            final var chat = createChat();
            chat.validateParticipant(playerOne);
        }

        @Test
        @DisplayName("should throw for non-participant")
        void validateParticipant_invalid_throws() {

            final var chat = createChat();
            final var stranger = PlayerId.generate();

            assertThatThrownBy(() -> chat.validateParticipant(stranger))
                .isInstanceOf(PlayerNotInChatException.class);
        }

    }

    @Nested
    @DisplayName("hasPlayer")
    class HasPlayer {

        @Test
        @DisplayName("should return true for participant")
        void hasPlayer_participant_returnsTrue() {

            final var chat = createChat();
            assertThat(chat.hasPlayer(playerOne)).isTrue();
        }

        @Test
        @DisplayName("should return false for non-participant")
        void hasPlayer_nonParticipant_returnsFalse() {

            final var chat = createChat();
            assertThat(chat.hasPlayer(PlayerId.generate())).isFalse();
        }

    }

}
