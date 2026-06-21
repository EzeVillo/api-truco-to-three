# Envido del bot con la partida empatada en punto (2-2)

> Decisión de dominio no obvia. Verificá contra el estado actual del código antes de asumir que
> sigue vigente.

Cuando ambos jugadores están a un punto de ganar (típicamente **2-2** en una partida a 3), la regla
de **punto exacto** invierte el valor del envido: el envido vale 2 → quien lo gana llega a 4 y **se
pasa (pierde)**, mientras que la falta vale 1 → quien la gana llega a 3 y **gana**. Por eso, en ese
escenario el bot (`EnvidoDecisionPolicy.decideBothAtMatchPoint`) **siempre canta**, sin intervención
de la personalidad, eligiendo nivel según la probabilidad real de ganar el tanto:

- `EnvidoProbabilityCalculator` la calcula **en vivo** enumerando el mazo: descuenta las cartas
  propias (que el rival no puede tener) y, si el bot es pie y el rival ya jugó, condiciona la mano
  rival a contener esa carta (y la descuenta también). El cruce del 50 % cae en ~22 de envido.
- `P ≥ 0,5` → **falta envido** (es favorito, quiere ganar el tanto y llegar a 3). `P < 0,5` →
  **envido** como trampa (quiere que el rival gane el tanto, llegue a 4 y se pase).
- Entra **siempre como mano**. **Como pie** solo entra si el rival jugó una carta que el bot **no
  puede matar** (si la puede matar conserva chances de truco y cae en la lógica habitual).
