package com.villo.truco.campaign.infrastructure.websocket.dto;

import java.util.Map;

public record CampaignWsEvent(String eventType, long timestamp, Map<String, Object> payload) {

}
