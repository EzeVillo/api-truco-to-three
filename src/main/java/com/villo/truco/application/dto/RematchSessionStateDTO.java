package com.villo.truco.application.dto;

import com.villo.truco.domain.model.rematch.valueobjects.RematchPlayerChoice;
import com.villo.truco.domain.model.rematch.valueobjects.RematchSessionStatus;
import java.time.Instant;

public record RematchSessionStateDTO(String sessionId, String originMatchId, String playerOneId,
                                     String playerTwoId, RematchSessionStatus status,
                                     RematchPlayerChoice playerOneChoice,
                                     RematchPlayerChoice playerTwoChoice, Instant expiresAt,
                                     String resultMatchId) {

}
