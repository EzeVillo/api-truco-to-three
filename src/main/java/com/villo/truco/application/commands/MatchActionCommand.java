package com.villo.truco.application.commands;

import com.villo.truco.domain.shared.valueobjects.MatchId;
import com.villo.truco.domain.shared.valueobjects.PlayerId;

/**
 * Marca los commands que representan una acción jugable sobre una partida (jugar carta, cantar o
 * responder truco/envido, irse al mazo). Permite que {@code GameplayRecordingDecorator} registre la
 * decisión de forma uniforme sin conocer el tipo concreto. Implementada por los 6 commands de acción.
 */
public interface MatchActionCommand {

  MatchId matchId();

  PlayerId playerId();

}
