package com.villo.truco.campaign.domain.model.valueobjects;

import com.villo.truco.campaign.domain.model.exceptions.InvalidCampaignRivalRecordException;

public record CampaignRivalRecord(int wins, int losses) {

  public static final CampaignRivalRecord EMPTY = new CampaignRivalRecord(0, 0);

  public CampaignRivalRecord {

    if (wins < 0 || losses < 0) {
      throw new InvalidCampaignRivalRecordException(wins, losses);
    }
  }

  public CampaignRivalRecord withWin() {

    return new CampaignRivalRecord(this.wins + 1, this.losses);
  }

  public CampaignRivalRecord withLoss() {

    return new CampaignRivalRecord(this.wins, this.losses + 1);
  }

  public boolean hasWin() {

    return this.wins > 0;
  }

  public int net() {

    return this.wins - this.losses;
  }

  public boolean isFavorableBy(final int threshold) {

    return this.net() >= threshold;
  }

}
