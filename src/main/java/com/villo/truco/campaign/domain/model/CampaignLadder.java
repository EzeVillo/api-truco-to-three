package com.villo.truco.campaign.domain.model;

import com.villo.truco.campaign.domain.model.exceptions.CampaignLadderEmptyException;
import com.villo.truco.campaign.domain.model.exceptions.CampaignLadderPointsNotStrictlyDecreasingException;
import com.villo.truco.campaign.domain.model.exceptions.CampaignLadderPositionsNotContiguousException;
import com.villo.truco.campaign.domain.model.valueobjects.CampaignPoints;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

public record CampaignLadder(List<CampaignBot> bots) {

  public CampaignLadder(final List<CampaignBot> bots) {

    Objects.requireNonNull(bots, "bots cannot be null");

    if (bots.isEmpty()) {
      throw new CampaignLadderEmptyException();
    }

    for (var index = 0; index < bots.size(); index++) {
      final var bot = bots.get(index);
      if (bot.position() != index + 1) {
        throw new CampaignLadderPositionsNotContiguousException(bot.position(), index);
      }
      if (index > 0 && bots.get(index - 1).points() <= bot.points()) {
        throw new CampaignLadderPointsNotStrictlyDecreasingException(bot.position(),
            bots.get(index - 1).position());
      }
    }

    this.bots = List.copyOf(bots);
  }

  public int totalBots() {

    return this.bots.size();
  }

  public int positionFor(final CampaignPoints playerPoints) {

    final var botsAbove = (int) this.bots.stream()
        .filter(bot -> bot.points() >= playerPoints.value()).count();
    return botsAbove + 1;
  }

  public Optional<CampaignBot> nextRival(final CampaignPoints playerPoints) {

    final var playerPosition = this.positionFor(playerPoints);
    if (playerPosition == 1) {
      return Optional.empty();
    }
    return Optional.of(this.bots.get(playerPosition - 2));
  }

  public OptionalInt pointsToOvertakeNextRival(final CampaignPoints playerPoints) {

    return this.nextRival(playerPoints)
        .map(rival -> OptionalInt.of(rival.points() + 1 - playerPoints.value()))
        .orElseGet(OptionalInt::empty);
  }

  public Optional<CampaignBot> findBot(final PlayerId botId) {

    return this.bots.stream().filter(bot -> bot.playerId().equals(botId)).findFirst();
  }

}
