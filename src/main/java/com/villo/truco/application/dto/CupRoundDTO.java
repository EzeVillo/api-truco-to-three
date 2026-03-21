package com.villo.truco.application.dto;

import java.util.List;

public record CupRoundDTO(int roundNumber, String roundName, List<CupBoutDTO> bouts) {

}
