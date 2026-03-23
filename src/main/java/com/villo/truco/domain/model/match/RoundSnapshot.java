package com.villo.truco.domain.model.match;

import com.villo.truco.domain.model.match.valueobjects.RoundId;
import com.villo.truco.domain.model.match.valueobjects.RoundStatus;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;

public record RoundSnapshot(RoundId id, int roundNumber, PlayerId mano, PlayerId playerOne,
                            PlayerId playerTwo, HandSnapshot handPlayerOne,
                            HandSnapshot handPlayerTwo, List<PlayedHandSnapshot> playedHands,
                            List<CardPlaySnapshot> currentHandCards,
                            TrucoSnapshot trucoStateMachine, EnvidoSnapshot envidoStateMachine,
                            RoundStatus status, PlayerId currentTurn, PlayerId turnBeforeTrucoCall,
                            PlayerId turnBeforeEnvidoCall) {

}
