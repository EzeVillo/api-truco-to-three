# Feature Specification: Motor de decisión del bot por aritmética del match a 3

**Feature Branch**: `017-bot-decision-engine`

**Created**: 2026-06-17

**Status**: Draft

**Input**: User description: "Rediseñar el sistema de toma de decisiones de los bots de truco para
un match a 3 puntos exactos. La mayoría de las decisiones están forzadas por la aritmética del
marcador, no por estilo ni azar. Dos ideas atraviesan el sistema: (1) manejar el 'pasarse de 3'
cantando/respondiendo para que el rival se pase o deje ganar; (2) encerrar al rival con las cartas
para que no pueda 'quiero y me voy al mazo'. El sistema debe deducir las jugadas de la aritmética,
recurriendo a probabilidades solo cuando la decisión depende de algo oculto (el tanto del rival o
una mano no jugada)."

## Aclaraciones del dominio

Conceptos que se usan en toda la spec (alineados con las reglas de `AGENTS.md`):

- **Match a 3 / punto exacto**: una partida se gana llegando a **exactamente 3 puntos**;
  **pasarse de 3 hace perder**. Por eso muchas jugadas no buscan ganar un tanto, sino forzar que el
  rival lo gane y se pase, o dejarlo sin respuesta legal favorable.
- **QYMVAM** ("quiero y me voy al mazo"): respuesta a un canto de truco. **No es legal si el
  jugador no tiene cartas en la mano.**
- **Información cierta (determinística)**: el bot conoce con certeza su propia mano, las cartas ya
  jugadas por ambos, cuántas cartas le quedan al rival y el marcador exacto. Saber si una de sus
  cartas **mata una carta que el rival ya jugó** es determinístico, no probabilístico.
- **Información oculta (probabilística)**: el bot **no** ve el tanto de envido del rival ni las
  cartas que el rival todavía no jugó. Solo en estos dos casos se recurre a la calculadora de
  probabilidad del tanto.
- **Encierro de cartas**: cuando la línea de cartas deja al bot con carta y al rival sin cartas, el
  bot puede cantar truco/retruco/vale cuatro sabiendo que el rival **no puede QYMVAM**. A veces
  llegar a esa posición exige **no cantar ahora y avanzar**.
- **Valor esperado (VE)**: el bot elige la jugada de mayor valor esperado. No evita toda rama donde
  podría pasarse de 3; acepta una rama improbable de derrota si la rama probable lo hace ganar.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El bot fuerza al rival a pasarse del límite (Priority: P1)

El motor del bot evalúa, antes de cada canto o respuesta, la aritmética exacta del marcador para
elegir la jugada que deja al rival sin salida: o el rival gana el tanto y **se pasa de 3** (pierde),
o el rival no quiere/no puede aceptar sin perder. Esto cubre tanto cantos de envido como de truco y
respuestas a cantos del rival.

**Why this priority**: Es el corazón del rediseño y la fuente principal de victorias del bot en el
formato a 3. Sin esta lógica, el bot no aprovecha la mecánica de punto exacto.

**Independent Test**: Se puede probar de forma aislada construyendo posiciones de marcador + cartas
conocidas y verificando que el bot elige la respuesta/canto que lleva al rival por encima de 3 o lo
deja sin opción favorable, sin depender del resto del sistema.

**Acceptance Scenarios**:

1. **Given** marcador 2-2 y el bot puede cantar el tanto, **When** el bot tiene **más probabilidad
   de perder** el tanto que de ganarlo, **Then** canta **envido** (busca que el rival gane el tanto
   y pase de 3 a 4).
2. **Given** marcador 2-2 y el bot puede cantar el tanto, **When** el bot tiene **más probabilidad
   de ganar** el tanto que de perderlo, **Then** canta **falta envido** (apunta a llegar exacto a
   3).
3. **Given** que el bot debe **responder** a un canto del rival, **When** decir **"no quiero"** hace
   que el rival se pase de 3, **Then** responde "no quiero".
4. **Given** que el bot debe responder a un canto del rival y tiene cartas en la mano, **When**
   **QYMVAM** hace que el rival se pase de 3, **Then** responde QYMVAM.
5. **Given** marcador 2-1 arriba y el rival podría hacerle la jugada de encierro, **When** la
   probabilidad de **perder** el tanto es muy alta, **Then** el bot canta un nivel de envido que
   obliga al rival a aceptar de modo que, si el rival gana, **se pasa de 3**.

---

### User Story 2 - El bot encierra al rival dejándolo sin cartas (Priority: P1)

El motor reconoce las líneas de cartas en las que, al avanzar las manos, el bot termina con carta y
el rival sin cartas. En esa posición canta truco/retruco/vale cuatro sabiendo que el rival **no
puede QYMVAM**; si el rival acepta, el bot puede irse al mazo en su turno. El motor entiende que
**a veces no debe cantar ahora**, sino avanzar para llegar a la posición de encierro.

**Why this priority**: Es la segunda gran palanca del formato y, combinada con la US1, define el
nivel de juego del bot. Es independiente porque se basa en información totalmente determinística
(cartas jugadas y cartas restantes del rival).

**Independent Test**: Se puede probar con manos donde el bot mata la primera carta del rival y se
verifica que (a) **no canta** y avanza cuando el canto inmediato no es óptimo, y (b) **sí canta** el
nivel de truco correspondiente una vez que el rival se quedó sin cartas.

**Acceptance Scenarios**:

1. **Given** marcador 2-2, el bot no es mano, el rival ya tiró carta y el bot la **mata** con su
   primera carta, **When** el bot decide, **Then** **no canta**: avanza para llegar a la 3ª mano con
   carta mientras el rival se queda sin cartas.
2. **Given** la 3ª mano con el bot con carta y el rival sin cartas, **When** la carta del bot
   **mata** la última carta del rival, **Then** el bot gana la mano.
3. **Given** la 3ª mano con el bot con carta y el rival sin cartas, **When** la carta del bot **no
   mata** la del rival, **Then** el bot canta truco (el rival no puede QYMVAM) y, si el rival
   acepta,
   el bot se va al mazo.
4. **Given** marcador 2-1 abajo, empieza el rival y el bot **mata** su primera carta, **When** el
   bot decide, **Then** canta envido para obligar al rival a no querer (aceptar lo haría perder
   igual): el bot suma 1 y queda 2-2, y a partir de ahí aplica la lógica de encierro.

---

### User Story 3 - El bot usa la escalera de cantos para encerrar desde el inicio (Priority: P2)

Cuando el bot controla la mano y tiene niveles de truco altos disponibles (retruco, vale cuatro),
usa la misma lógica de encierro desde marcadores tempranos: planifica la secuencia de cantos para
dejar al rival sin respuesta legal cuando se quede sin cartas, en lugar de cantar todo de una.

**Why this priority**: Extiende la lógica de encierro a más posiciones (1-1, 0-0) y mejora el juego
en partidas que arrancan parejas, pero el valor incremental sobre US1+US2 es menor.

**Independent Test**: Se puede probar con posiciones 1-1 y 0-0 verificando que el bot acepta o
escala el canto que conduce al encierro y no toma una rama de mayor riesgo.

**Acceptance Scenarios**:

1. **Given** marcador 1-1, el rival canta truco y el bot **ganó** la primera mano (o la controla),
   **When** el bot decide, **Then** acepta (quiero) porque cuando el rival se quede sin cartas le
   canta el retruco y el rival no podrá QYMVAM.
2. **Given** marcador 1-1, el rival canta truco y la primera mano **todavía no se jugó**, **When**
   la probabilidad de que la carta alta del bot gane esa mano es favorable, **Then** el bot acepta
   apostando a ganarla.
3. **Given** marcador 0-0, el bot tiene el vale cuatro disponible y controla la mano, **When** el
   bot decide, **Then** encadena la escalera de cantos para dejar al rival sin respuesta cuando se
   quede sin cartas.

---

### Edge Cases

- **Empate de probabilidad en 2-2** (50%/50% ganar vs. perder el tanto): el sistema debe tener una
  regla de desempate determinística y documentada (por defecto: tratar el empate como "no más
  probable perder", es decir, ir a falta envido).
- **El bot no tiene cartas para QYMVAM**: nunca debe elegir QYMVAM; debe caer a la mejor respuesta
  legal alternativa.
- **Ninguna respuesta deja al rival sin salida**: cuando la aritmética no fuerza nada, el bot decide
  por valor esperado usando la probabilidad del tanto / mano no jugada.
- **El propio bot se pasaría de 3 al aceptar/ganar**: una jugada que hace que el **bot** se pase no
  debe elegirse salvo que sea la rama improbable de una decisión cuyo VE sigue siendo ganador
  (regla general, Caso 8).
- **Cartas iguales / parda** en una mano que define el encierro: el resultado de "mata o no mata"
  debe contemplar la parda y resolver según el reglamento del proyecto.
- **El rival canta justo cuando al bot le conviene avanzar**: el bot debe poder responder de forma
  que conserve la posición de encierro en lugar de resolver el tanto antes de tiempo.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El motor de decisión DEBE elegir cada canto y cada respuesta resolviendo primero la
  **aritmética del marcador a 3** (puntos del bot, puntos del rival, puntos en juego según el canto
  aceptado o rechazado) antes de recurrir a cualquier heurística de estilo o azar.
- **FR-002**: El motor DEBE distinguir entre información **determinística** (mano propia, cartas
  jugadas, cartas restantes del rival, marcador) e información **oculta** (tanto del rival, cartas
  no jugadas del rival), y DEBE usar probabilidad **solo** para resolver lo que depende de
  información oculta.
- **FR-003**: El motor DEBE determinar de forma determinística si una carta propia **mata** una
  carta que el rival **ya jugó**, sin usar probabilidad.
- **FR-004**: Cuando exista una respuesta legal que haga que el **rival se pase de 3** (por aceptar,
  por ganar el tanto, o por el valor del canto rechazado), el motor DEBE preferir esa respuesta.
- **FR-005**: El motor DEBE poder elegir **"no quiero"** cuando rechazar el canto hace que el rival
  se pase de 3.
- **FR-006**: El motor DEBE poder elegir **QYMVAM** cuando ello hace que el rival se pase de 3, y
  DEBE elegir QYMVAM **solo si el bot tiene cartas en la mano** (nunca cuando es ilegal).
- **FR-007**: En 2-2, cuando el bot puede cantar el tanto, el motor DEBE cantar **sí o sí**,
  eligiendo
  **envido** si es **más probable perder** el tanto que ganarlo, o **falta envido** si es **más
  probable ganarlo** que perderlo.
- **FR-008**: El motor DEBE reconocer las posiciones de **encierro** (bot con carta y rival sin
  cartas) y, en ellas, cantar el nivel de truco que el rival no puede contestar con QYMVAM.
- **FR-009**: El motor DEBE poder **postergar un canto y avanzar** cuando avanzar conduce a una
  posición de encierro de mayor valor esperado que cantar de inmediato.
- **FR-010**: En la mano que cierra el encierro, el motor DEBE ganar la mano si su carta **mata** la
  última carta del rival, y si **no la mata** DEBE cantar truco y luego irse al mazo si el rival
  acepta.
- **FR-011**: En 2-1 abajo, cuando empieza el rival y el bot mata su primera carta, el motor DEBE
  poder cantar envido para obligar al rival a no querer, llevando el marcador a 2-2 y habilitando la
  lógica de encierro.
- **FR-012**: En 2-1 arriba, cuando el rival podría aplicar la jugada de encierro y la probabilidad
  de **perder el tanto es muy alta**, el motor DEBE poder cantar un nivel de envido que obligue al
  rival a aceptar de modo que, si el rival gana, **se pase de 3**.
- **FR-013**: En posiciones 1-1 y 0-0 con niveles altos de truco disponibles, el motor DEBE poder
  aceptar o escalar los cantos que conducen al encierro en lugar de tomar ramas de mayor riesgo.
- **FR-014**: Cuando una decisión de truco depende de una **mano todavía no jugada**, el motor DEBE
  estimar mediante probabilidad si la **carta alta** del bot ganará esa mano y decidir según ese
  valor.
- **FR-015**: El motor DEBE elegir siempre la jugada de **mayor valor esperado** y NUNCA elegir una
  jugada cuyo **resultado esperado** sea perder; aceptar una rama improbable de derrota es válido si
  la rama probable hace ganar.
- **FR-016**: El comportamiento del motor DEBE **derivarse de la aritmética y la posición de
  cartas**, no de reglas cableadas caso por caso; agregar un caso nuevo del mismo principio NO debe
  requerir una rama de código específica para ese caso.
- **FR-017**: El motor DEBE tener una regla de **desempate determinística y documentada** para
  cuando las probabilidades de ganar y perder el tanto sean iguales.
- **FR-018**: El motor DEBE seguir produciendo una jugada legal válida en toda posición posible del
  match a 3, incluyendo aquellas en las que ninguna jugada fuerza al rival (decidiendo por VE).

### Key Entities *(include if feature involves data)*

- **Posición del match (visión del bot)**: marcador exacto del bot y del rival, puntos para ganar
  (3), número de mano en curso, quién es mano, cartas propias en la mano, carta(s) ya jugada(s) por
  el rival y cantidad de cartas que le quedan al rival, y el estado de cantos pendientes (truco /
  envido a responder y niveles disponibles).
- **Resultado aritmético de un canto**: para cada nivel de canto disponible, los puntos que sumaría
  cada lado si se acepta y se gana, si se acepta y se pierde, o si se rechaza; usado para detectar
  "el rival se pasa" / "el bot se pasa".
- **Estimación de probabilidad del tanto**: probabilidad de que el bot gane el tanto del envido dado
  su propio tanto, si es mano y la carta que el rival ya mostró; única fuente de incertidumbre junto
  con la mano no jugada.
- **Estimación de probabilidad de mano no jugada**: probabilidad de que la carta alta del bot gane
  una mano que el rival aún no jugó.
- **Decisión del bot**: la acción elegida (cantar envido/nivel, cantar truco/nivel, responder
  quiero/no quiero/QYMVAM, jugar carta, avanzar sin cantar, irse al mazo) con su justificación
  aritmética/probabilística.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En el 100% de las posiciones donde existe una jugada legal que hace que el rival se
  pase de 3 o lo deja sin respuesta favorable, el bot elige esa jugada.
- **SC-002**: El bot nunca elige QYMVAM en una posición en la que no tiene cartas (0 ocurrencias en
  todos los escenarios de prueba).
- **SC-003**: Los 8 casos concretos descritos por el usuario (2-2 cantar el tanto, encierro sin
  cartas, 2-1 abajo, 2-1 arriba, respuesta que pasa al rival, 1-1 con truco, 0-0 con vale cuatro,
  regla general de VE) están cubiertos por escenarios verificables y el bot produce la jugada
  indicada en cada uno.
- **SC-004**: El bot nunca realiza una jugada cuyo resultado esperado sea perder (0 jugadas de VE
  negativo en los escenarios de prueba), permitiendo ramas improbables de derrota dentro de una
  decisión de VE positivo.
- **SC-005**: El uso de probabilidad se limita a decisiones que dependen de información oculta; las
  decisiones que solo dependen de información cierta se resuelven de forma determinística y
  repetible
  (mismo input → misma salida).
- **SC-006**: Incorporar un nuevo caso del mismo principio aritmético se cubre sin agregar una rama
  de decisión específica para ese caso (se valida revisando que los casos nuevos quedan resueltos
  por
  la lógica general existente).
- **SC-007**: En enfrentamientos del bot rediseñado contra el bot anterior en match a 3, el bot
  rediseñado gana una proporción de partidas claramente mayor (objetivo: ≥ 60%) en una muestra
  significativa de partidas.

## Assumptions

- El motor aplica a los matches a **3 puntos** (formato del proyecto, `pointsToWin = 3`); la lógica
  se expresa en función del marcador y los puntos para ganar, sin cablear el literal 3.
- El rediseño aplica a **todos los bots** del juego (casual y campaña). Las personalidades/estilos
  existentes (envidoso, mentiroso, pescador, etc.) **solo modulan las decisiones genuinamente libres
  o probabilísticas**; cuando la aritmética fuerza una jugada, la aritmética manda por encima del
  estilo.
- Ya existe una **calculadora de probabilidad del tanto** reutilizable; el rediseño la usa como
  única fuente de azar para el envido y se asume disponible una estimación equivalente para la "mano
  no jugada".
- "Probabilidad muy alta de perder el tanto" (Caso 4) se interpreta como un umbral configurable; se
  asume un valor por defecto razonable, ajustable, y no como una constante cableada.
- El reglamento de mata/parda y de legalidad de cantos/QYMVAM ya está implementado en el dominio del
  match; este motor **consume** esas reglas, no las redefine.
- El alcance es la **toma de decisión del bot** (qué jugada elegir). No incluye cambios de UI,
  endpoints nuevos ni cambios al modelo de scoring del match.
