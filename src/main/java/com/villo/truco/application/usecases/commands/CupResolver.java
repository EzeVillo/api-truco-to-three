package com.villo.truco.application.usecases.commands;

import com.villo.truco.application.exceptions.CupNotFoundException;
import com.villo.truco.domain.model.cup.Cup;
import com.villo.truco.domain.model.cup.valueobjects.CupId;
import com.villo.truco.domain.ports.CupQueryRepository;
import com.villo.truco.domain.shared.valueobjects.JoinCode;
import java.util.Objects;

public final class CupResolver {

  private final CupQueryRepository cupQueryRepository;

  public CupResolver(final CupQueryRepository cupQueryRepository) {

    this.cupQueryRepository = Objects.requireNonNull(cupQueryRepository);
  }

  public Cup resolve(final CupId cupId) {

    return this.cupQueryRepository.findById(cupId)
        .orElseThrow(() -> new CupNotFoundException(cupId));
  }

  public Cup resolve(final JoinCode joinCode) {

    return this.cupQueryRepository.findByJoinCode(joinCode)
        .orElseThrow(() -> new CupNotFoundException(joinCode));
  }

}
