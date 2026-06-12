package com.villo.truco.campaign.domain.model.exceptions;

import com.villo.truco.domain.shared.DomainException;

public final class InvalidCampaignVictoryException extends DomainException {

  public InvalidCampaignVictoryException(final int gamesWonWinner, final int gamesWonLoser) {

    super("campaign victory games cannot be negative: winner=" + gamesWonWinner + ", loser="
        + gamesWonLoser);
  }

}
