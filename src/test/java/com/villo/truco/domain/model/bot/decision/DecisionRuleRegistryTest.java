package com.villo.truco.domain.model.bot.decision;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.domain.model.bot.valueobjects.BotAction;
import com.villo.truco.domain.model.bot.valueobjects.BotCard;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.EnvidoContext;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.GameContext;
import com.villo.truco.domain.model.bot.valueobjects.BotMatchView.TrucoContext;
import com.villo.truco.domain.shared.cards.valueobjects.Card;
import com.villo.truco.domain.shared.cards.valueobjects.Suit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DecisionRuleRegistryTest {

  private static final BotCard ANCHO = new BotCard(14, Card.of(Suit.ESPADA, 1));
  private static final BotAction JUGAR_ANCHO = new BotAction.PlayCard(ANCHO);

  private static DecisionContext ctx() {

    final var game = new GameContext(List.of(ANCHO), 0, 0, null, 0, 0, false, true, false, false,
        3, 1);
    final var view = new BotMatchView(game, new TrucoContext(null, List.of(), null),
        new EnvidoContext(List.of(), List.of(), List.of(), null));
    final var arithmetic = new MatchArithmetic(0, 0, 3);
    final var lock = new CardLockAnalyzer(game);
    final var tanto = TantoProbabilityProvider.withKnownProbability(0.5);
    final var unplayed = new UnplayedHandProbability(List.of(ANCHO), null);
    return new DecisionContext(view, arithmetic, lock, tanto, unplayed);
  }

  @Test
  void evaluaPorPrioridad_primeraReglaQueOpina_gana() {

    final DecisionRule alta = new DecisionRule() {
      @Override
      public Optional<BotAction> apply(final DecisionContext ctx) {
        return Optional.of(JUGAR_ANCHO);
      }

      @Override
      public int priority() {
        return 1;
      }

      @Override
      public String name() {
        return "alta";
      }
    };

    final DecisionRule baja = new DecisionRule() {
      @Override
      public Optional<BotAction> apply(final DecisionContext ctx) {
        return Optional.of(new BotAction.Fold());
      }

      @Override
      public int priority() {
        return 100;
      }

      @Override
      public String name() {
        return "baja";
      }
    };

    final var registry = new DecisionRuleRegistry(List.of(baja, alta));
    final var result = registry.decide(ctx());
    assertThat(result).isEqualTo(JUGAR_ANCHO);
  }

  @Test
  void cuandoReglaAltaNoOpina_siguiente_resuelve() {

    final DecisionRule sinOpinion = new DecisionRule() {
      @Override
      public Optional<BotAction> apply(final DecisionContext ctx) {
        return Optional.empty();
      }

      @Override
      public int priority() {
        return 1;
      }

      @Override
      public String name() {
        return "sin-opinion";
      }
    };

    final DecisionRule fallback = new DecisionRule() {
      @Override
      public Optional<BotAction> apply(final DecisionContext ctx) {
        return Optional.of(JUGAR_ANCHO);
      }

      @Override
      public int priority() {
        return 100;
      }

      @Override
      public String name() {
        return "fallback";
      }
    };

    final var registry = new DecisionRuleRegistry(List.of(sinOpinion, fallback));
    final var result = registry.decide(ctx());
    assertThat(result).isEqualTo(JUGAR_ANCHO);
  }

  @Test
  void siempreResuelve_fallbackGarantizaAccion() {

    final DecisionRule siempre = new DecisionRule() {
      @Override
      public Optional<BotAction> apply(final DecisionContext ctx) {
        return Optional.of(JUGAR_ANCHO);
      }

      @Override
      public int priority() {
        return 1000;
      }

      @Override
      public String name() {
        return "siempre";
      }
    };

    final var registry = new DecisionRuleRegistry(List.of(siempre));
    assertThat(registry.decide(ctx())).isInstanceOf(BotAction.PlayCard.class);
  }

  @Test
  void registryOrdenaPorPrioridad_independientementeDelOrdenDeEntrada() {

    final var acciones = new java.util.ArrayList<String>();

    final DecisionRule segunda = new DecisionRule() {
      @Override
      public Optional<BotAction> apply(final DecisionContext ctx) {
        acciones.add("segunda");
        return Optional.empty();
      }

      @Override
      public int priority() {
        return 20;
      }

      @Override
      public String name() {
        return "segunda";
      }
    };

    final DecisionRule primera = new DecisionRule() {
      @Override
      public Optional<BotAction> apply(final DecisionContext ctx) {
        acciones.add("primera");
        return Optional.empty();
      }

      @Override
      public int priority() {
        return 10;
      }

      @Override
      public String name() {
        return "primera";
      }
    };

    final DecisionRule ultima = new DecisionRule() {
      @Override
      public Optional<BotAction> apply(final DecisionContext ctx) {
        return Optional.of(JUGAR_ANCHO);
      }

      @Override
      public int priority() {
        return 1000;
      }

      @Override
      public String name() {
        return "ultima";
      }
    };

    final var registry = new DecisionRuleRegistry(List.of(segunda, primera, ultima));
    registry.decide(ctx());
    assertThat(acciones).containsExactly("primera", "segunda");
  }

}
