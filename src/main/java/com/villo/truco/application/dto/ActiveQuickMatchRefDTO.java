package com.villo.truco.application.dto;

import java.time.Instant;

public record ActiveQuickMatchRefDTO(String status, Instant enqueuedAt) {

}
