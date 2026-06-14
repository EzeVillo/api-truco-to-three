package com.villo.truco.application.dto;

import java.util.List;

public record BotCatalogDTO(List<BotProfileDTO> casual, List<BotProfileDTO> campaignUnlocked) {

}
