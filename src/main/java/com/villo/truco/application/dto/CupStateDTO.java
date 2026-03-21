package com.villo.truco.application.dto;

import java.util.List;

public record CupStateDTO(String cupId, String status, List<CupRoundDTO> rounds, String champion) {

}
