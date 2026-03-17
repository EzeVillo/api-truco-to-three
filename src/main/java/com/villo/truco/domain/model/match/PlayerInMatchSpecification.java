package com.villo.truco.domain.model.match;

import com.villo.truco.domain.shared.valueobjects.PlayerId;

final class PlayerInMatchSpecification {

  private PlayerInMatchSpecification() {

  }

  static boolean isSatisfiedBy(final PlayerId candidate, final PlayerId playerOne,
      final PlayerId playerTwo) {

    return candidate.equals(playerOne) || candidate.equals(playerTwo);
  }

}