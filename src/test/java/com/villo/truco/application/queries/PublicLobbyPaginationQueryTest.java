package com.villo.truco.application.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.exceptions.InvalidCursorPageRequestException;
import com.villo.truco.domain.shared.pagination.PublicLobbyCursor;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Public lobby pagination queries")
class PublicLobbyPaginationQueryTest {

  private static final String PLAYER_ID = "11111111-1111-1111-1111-111111111111";

  @Test
  @DisplayName("limit mayor a 100 se rechaza")
  void rejectsLimitAboveMaximum() {

    assertThatThrownBy(() -> new GetPublicLeaguesQuery(PLAYER_ID, 101, null)).isInstanceOf(
        InvalidCursorPageRequestException.class).hasMessage("limit must be between 1 and 100");
  }

  @Test
  @DisplayName("cursor invalido se rechaza")
  void rejectsInvalidCursor() {

    assertThatThrownBy(() -> new GetPublicCupsQuery(PLAYER_ID, 20, "not-a-cursor")).isInstanceOf(
        InvalidCursorPageRequestException.class).hasMessage("after cursor is invalid");
  }

  @Test
  @DisplayName("cursor valido se preserva")
  void acceptsEncodedCursor() {

    final var cursor = new PublicLobbyCursor(Instant.parse("2026-04-03T12:30:00Z"),
        UUID.fromString("22222222-2222-2222-2222-222222222222")).encode();

    final var query = new GetPublicMatchesQuery(PLAYER_ID, 25, cursor);

    assertThat(query.pageQuery().limit()).isEqualTo(25);
    assertThat(query.pageQuery().after()).isEqualTo(cursor);
  }

}
