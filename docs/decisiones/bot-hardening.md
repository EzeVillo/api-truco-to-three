# Debilidad de los bots en punto-exacto y meta-estrategia del humano

> Análisis de dominio (base del hardening del bot). Verificá contra el estado actual del código y de
> los datos antes de asumir que sigue vigente.

En la variante **a 3 / punto exacto**, los bots tienen una grieta conceptual que el usuario
(EzeVillo) explota para ~95 % de win rate en producción. Este doc describe la falla y la
meta-estrategia que la aprovecha, como base para endurecer el motor de decisión del bot.

## La falla central (signo invertido)

El bot evalúa local *"¿gano esto? → si sí, quiero/canto; si no, no quiero"*. Pero en punto-exacto,
estando en **2**, ganar = pasarse de 3 = **perder**. El bot tiene invertido el signo del valor en
toda la "zona de parálisis" y no modela apuntar a un **puntaje exacto** en vez de maximizar: en 2 lo
único que le conviene es ganar la ronda pelada por 1 (cae justo en 3), pero no distingue "quiero
exactamente 1, no 2".

> El caso 2-2 del envido ya está corregido en `EnvidoDecisionPolicy.decideBothAtMatchPoint` — ver
> [`bot-envido.md`](bot-envido.md). El resto de la zona de parálisis sigue sin modelarse.

**Lo que el bot NO modela (las patas de la meta del humano):**

1. EV en función del score y del objetivo "3 exacto".
2. Que lo están empujando a la zona de parálisis (regalos tempranos = manipulación, no regalo).
3. Orden de mano/turno de la ronda decisiva (juega cada ronda como aislada).
4. Cuidar la 1ª mano y el recurso `QUIERO_Y_ME_VOY_AL_MAZO` (no disponible sin cartas en mano).

**Mejoras propuestas (orden de impacto):** (1) EV score-aware con objetivo 3 exacto; (2) detectar
manipulación/parálisis; (3) modelar orden de mano; (4) cuidar 1ª mano + recurso de mazo; (5)
cantar/aceptar por valor real, no por reflejo.

## Meta-estrategia del usuario (lo que explota la falla)

Es el juego *ideal* del usuario; no siempre se ejecuta exacto.

**Idea central:** le conviene ir **2-0 (o 2-1) abajo a propósito**. Con el bot en 2, casi todo lo
que el bot gane lo pasa de 3 (revienta) → el bot queda paralizado y no puede cantar ni aceptar nada
que valga ≥2. El usuario entrega esos 2 baratos vía `QUIERO_Y_ME_VOY_AL_MAZO` o `FOLD` temprano, y
después maniobra con libertad.

**Cuando empieza él (es mano):** va 2-0 abajo; en la 2ª ronda canta envido + truco para llegar a
2-2, y llega a la 3ª ronda sabiendo —por cómo cantó— que abre él:

- Con tantos altos → `FALTA_ENVIDO` (a 2-2 vale exactamente 1 → cae en 3).
- Con **pocos tantos → canta envido igual**, porque sabe que el bot le gana y **se pasa de 3**. Gana
  con la peor mano. (El golpe que ningún bot que maximiza tantos defiende.)

**Cuando empieza el bot:** mejor aún. Va 2-0 abajo; en la 2ª ronda (abre él) canta truco → 2-1
abajo; abre el bot y no puede cantar nada. El bot tira carta:

1. Si el usuario tiene **carta más alta**: canta envido y gana la 1ª mano. Tira 2ª; si el bot la
   mata, ok; el bot tira la 3ª; si el usuario no la mata, canta truco — yendo 2-2 el bot **no puede
   **
   `QUIERO_Y_ME_VOY_AL_MAZO` (regla: no disponible sin cartas), así que el usuario se va al mazo y
   gana. Todo habilitado por **ganar la primera mano**.
2. Si tiene **carta más baja**: dice truco, va a otra ronda donde abre él, y ahí redefine (envido
   para que se pase, o falta si tiene tanto).

**Regla de oro: ganar SIEMPRE la primera mano o irse al mazo.** Si tira la carta sin que el bot
tenga nada, el bot igual puede cantar `VALE_CUATRO` y el usuario no puede
`quiero y me voy al mazo` →
queda expuesto.

## Limitación de datos

El dump de `match_action_log` analizado (4 matches, contra un bot ya roto) solo confirma esto
parcialmente. La data que vale para cuantificar **en qué score + jugada** se rompe el bot es la de
producción (95 % WR del usuario).
