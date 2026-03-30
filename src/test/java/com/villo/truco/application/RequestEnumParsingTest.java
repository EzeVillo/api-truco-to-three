package com.villo.truco.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.application.commands.CallEnvidoCommand;
import com.villo.truco.application.commands.PlayCardCommand;
import com.villo.truco.application.exceptions.InvalidEnumValueException;
import com.villo.truco.application.queries.GetChatByParentQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Request enum parsing")
class RequestEnumParsingTest {

  private static final String MATCH_ID = "22222222-2222-2222-2222-222222222222";
  private static final String PLAYER_ID = "11111111-1111-1111-1111-111111111111";

  @Test
  @DisplayName("CallEnvidoCommand traduce enum invalido a excepcion custom")
  void callEnvidoCommandThrowsCustomException() {

    assertThatThrownBy(() -> new CallEnvidoCommand(MATCH_ID, PLAYER_ID, "INVALIDO")).isInstanceOf(
            InvalidEnumValueException.class).hasMessageContaining("field 'call'")
        .hasMessageContaining("INVALIDO");
  }

  @Test
  @DisplayName("PlayCardCommand traduce suit invalido a excepcion custom")
  void playCardCommandThrowsCustomException() {

    assertThatThrownBy(() -> new PlayCardCommand(MATCH_ID, PLAYER_ID, "INVALIDO", 1)).isInstanceOf(
            InvalidEnumValueException.class).hasMessageContaining("field 'suit'")
        .hasMessageContaining("ESPADA");
  }

  @Test
  @DisplayName("GetChatByParentQuery traduce parentType invalido a excepcion custom")
  void getChatByParentQueryThrowsCustomException() {

    assertThatThrownBy(
        () -> new GetChatByParentQuery("INVALIDO", MATCH_ID, PLAYER_ID)).isInstanceOf(
            InvalidEnumValueException.class).hasMessageContaining("field 'parentType'")
        .hasMessageContaining("MATCH");
  }

}
