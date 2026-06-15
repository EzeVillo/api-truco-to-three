package com.villo.truco.application.usecases.recording;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.commands.CallEnvidoCommand;
import com.villo.truco.application.commands.CallTrucoCommand;
import com.villo.truco.application.commands.FoldCommand;
import com.villo.truco.application.commands.PlayCardCommand;
import com.villo.truco.application.commands.RespondEnvidoCommand;
import com.villo.truco.application.commands.RespondTrucoCommand;
import com.villo.truco.domain.model.gameplay.RecordedActionType;
import com.villo.truco.domain.model.match.valueobjects.EnvidoCall;
import com.villo.truco.domain.model.match.valueobjects.EnvidoResponse;
import com.villo.truco.domain.model.match.valueobjects.TrucoResponse;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RecordedActionFactory")
class RecordedActionFactoryTest {

  private final RecordedActionFactory factory = new RecordedActionFactory();

  private final MatchId matchId = MatchId.generate();
  private final PlayerId playerId = PlayerId.generate();

  @Test
  @DisplayName("mapea jugar carta con su carta como detalle")
  void mapsPlayCardWithCardDetail() {

    final var card = Card.of(Suit.ESPADA, 7);

    final var action = this.factory.from(new PlayCardCommand(this.matchId, this.playerId, card));

    assertThat(action.type()).isEqualTo(RecordedActionType.PLAY_CARD);
    assertThat(action.detail()).isEqualTo(card);
  }

  @Test
  @DisplayName("mapea cantar truco sin detalle")
  void mapsCallTrucoWithoutDetail() {

    final var action = this.factory.from(new CallTrucoCommand(this.matchId, this.playerId));

    assertThat(action.type()).isEqualTo(RecordedActionType.CALL_TRUCO);
    assertThat(action.detail()).isNull();
  }

  @Test
  @DisplayName("mapea responder truco con la respuesta")
  void mapsRespondTrucoWithResponse() {

    final var response = TrucoResponse.values()[0];

    final var action =
        this.factory.from(new RespondTrucoCommand(this.matchId, this.playerId, response));

    assertThat(action.type()).isEqualTo(RecordedActionType.RESPOND_TRUCO);
    assertThat(action.detail()).isEqualTo(response);
  }

  @Test
  @DisplayName("mapea cantar envido con el canto")
  void mapsCallEnvidoWithCall() {

    final var call = EnvidoCall.values()[0];

    final var action = this.factory.from(new CallEnvidoCommand(this.matchId, this.playerId, call));

    assertThat(action.type()).isEqualTo(RecordedActionType.CALL_ENVIDO);
    assertThat(action.detail()).isEqualTo(call);
  }

  @Test
  @DisplayName("mapea responder envido con la respuesta")
  void mapsRespondEnvidoWithResponse() {

    final var response = EnvidoResponse.values()[0];

    final var action =
        this.factory.from(new RespondEnvidoCommand(this.matchId, this.playerId, response));

    assertThat(action.type()).isEqualTo(RecordedActionType.RESPOND_ENVIDO);
    assertThat(action.detail()).isEqualTo(response);
  }

  @Test
  @DisplayName("mapea irse al mazo sin detalle")
  void mapsFoldWithoutDetail() {

    final var action = this.factory.from(new FoldCommand(this.matchId, this.playerId));

    assertThat(action.type()).isEqualTo(RecordedActionType.FOLD);
    assertThat(action.detail()).isNull();
  }

}
