package com.villo.truco.campaign.infrastructure.bot;

import com.villo.truco.campaign.domain.model.CampaignBot;
import com.villo.truco.campaign.domain.model.CampaignLadder;
import com.villo.truco.campaign.domain.model.CampaignPointsCurve;
import com.villo.truco.campaign.domain.ports.CampaignLadderProvider;
import com.villo.truco.domain.model.bot.BotProfile;
import com.villo.truco.domain.model.bot.valueobjects.BotPersonality;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class CampaignBotCatalog implements CampaignLadderProvider {

  private static final int TOTAL_BOTS = 100;
  private static final int TOP_POSITION_POINTS = 43_200;
  private static final long NAME_SHUFFLE_SEED = 20_260_611L;
  private static final String BOT_ID_FORMAT = "c0000000-0000-0000-0000-%012d";

  private static final List<String> FIRST_NAMES = List.of("Cacho", "Tito", "Rulo", "Negra", "Beto",
      "Pocha", "Chiche", "Coco", "Tona", "Lalo");
  private static final List<String> LAST_NAMES = List.of("Medina", "Toledo", "Suárez", "Quiroga",
      "Ferreyra", "Acosta", "Bustos", "Ledesma", "Villalba", "Romero");

  private final CampaignLadder ladder;

  public CampaignBotCatalog() {

    final var displayNames = shuffledDisplayNames();
    final var bots = new ArrayList<CampaignBot>(TOTAL_BOTS);
    for (var position = 1; position <= TOTAL_BOTS; position++) {
      bots.add(new CampaignBot(botPlayerId(position), displayNames.get(position - 1), position,
          CampaignPointsCurve.pointsForPosition(position, TOTAL_BOTS, TOP_POSITION_POINTS)));
    }
    this.ladder = new CampaignLadder(bots);
  }

  private static PlayerId botPlayerId(final int position) {

    return PlayerId.of(String.format(BOT_ID_FORMAT, position));
  }

  private static List<String> shuffledDisplayNames() {

    final var names = new ArrayList<String>(FIRST_NAMES.size() * LAST_NAMES.size());
    for (final var lastName : LAST_NAMES) {
      for (final var firstName : FIRST_NAMES) {
        names.add(firstName + " " + lastName);
      }
    }
    Collections.shuffle(names, new Random(NAME_SHUFFLE_SEED));
    return names;
  }

  private static BotPersonality personalityFor(final int position) {

    final var difficulty = (double) (TOTAL_BOTS - position) / (TOTAL_BOTS - 1);
    final var jitter = new Random(position);

    return new BotPersonality(jittered(lerp(70, 45, difficulty), jitter),
        jittered(lerp(70, 25, difficulty), jitter), jittered(lerp(85, 35, difficulty), jitter),
        jittered(lerp(30, 70, difficulty), jitter), jittered(lerp(20, 90, difficulty), jitter));
  }

  private static int lerp(final int from, final int to, final double progress) {

    return (int) Math.round(from + (to - from) * progress);
  }

  private static int jittered(final int value, final Random random) {

    final var jitter = random.nextInt(21) - 10;
    return Math.clamp(value + jitter, 1, 100);
  }

  @Override
  public CampaignLadder ladder() {

    return this.ladder;
  }

  public List<BotProfile> botProfiles() {

    return this.ladder.bots().stream().map(
            bot -> new BotProfile(bot.playerId(), bot.displayName(), personalityFor(bot.position())))
        .toList();
  }

}
