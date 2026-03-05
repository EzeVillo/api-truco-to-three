package com.villo.truco.application.dto;

import com.villo.truco.domain.model.match.valueobjects.AvailableAction;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record AvailableActionDTO(String type, List<String> parameters) {

  public static List<AvailableActionDTO> fromActions(final List<AvailableAction> actions) {

    return actions.stream().collect(Collectors.groupingBy(a -> a.type().name(),
            Collectors.mapping(a -> a.getParameter().orElse(null), Collectors.toList()))).entrySet()
        .stream().map(e -> new AvailableActionDTO(e.getKey(),
            e.getValue().stream().filter(Objects::nonNull).toList())).toList();
  }

}
