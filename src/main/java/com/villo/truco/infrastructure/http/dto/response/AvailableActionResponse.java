package com.villo.truco.infrastructure.http.dto.response;

import com.villo.truco.application.dto.AvailableActionDTO;
import java.util.List;

public record AvailableActionResponse(String type, List<String> parameters) {

    public static AvailableActionResponse from(final AvailableActionDTO dto) {

        return new AvailableActionResponse(dto.type(), dto.parameters());
    }

}
