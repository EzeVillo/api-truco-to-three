package com.villo.truco.infrastructure.http.dto.request;

import java.util.List;

public record CreateTournamentRequest(List<String> playerIds) {

}
