package com.villo.truco.infrastructure.http.dto.response;

import java.time.Instant;

public record ErrorResponse(String errorCode, String message, Instant timestamp) {

}
