# Feature Specification: Recopilación de partidas para entrenamiento de bots

**Feature Branch**: `claude/bot-training-match-data-azo9no`

**Created**: 2026-06-15

**Status**: Draft

**Input**: User description: "Recopilación de partidas para entrenamiento de bots (gameplay
recording / event log append-only). Objetivo de esta primera iteración: solo recopilar por ahora —
empezar a registrar de forma persistente cada decisión jugable de una partida (humano y bot) para
más adelante analizar y mejorar los bots."

## Overview

Hoy, cada acción jugable de una partida (jugar carta, cantar/responder truco, cantar/responder
envido, irse al mazo) produce información de estado que se usa en tiempo real y luego **se descarta**.
No queda registro histórico de cómo se jugó una partida.

Esta feature introduce un **registro histórico append-only** ("log de partidas"): por cada decisión
jugable que ocurre en una partida se guarda, de forma permanente, **el estado completo de la partida
en ese momento** junto con **la acción elegida** y **quién la tomó** (jugador humano o bot). El
objetivo de esta primera iteración es exclusivamente **recopilar** datos confiables y completos —
sin comprometerse todavía con ningún uso analítico o de machine learning específico.

El valor: acumular, desde ya y de forma perpetua, un dataset de partidas reales que más adelante
permita entender cómo juega el humano, comparar contra cómo juegan los bots y, eventualmente,
mejorar o entrenar a los bots.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Registrar cada decisión jugable con su estado (Priority: P1)

Como dueño del producto, quiero que el sistema registre automáticamente cada decisión jugable de
toda partida —tanto las mías como las del bot— junto con el estado completo de la partida en el
instante de esa decisión, para acumular un historial fiel de cómo se juega.

**Why this priority**: Es el corazón de la feature y el MVP. Sin la captura confiable de pares
"estado + acción", no hay dataset y nada de lo demás (exportar, analizar) tiene sentido. Entrega
valor por sí sola: apenas esté, cada partida que juegue empieza a quedar guardada.

**Independent Test**: Jugar una partida completa contra un bot y verificar que quedó persistida una
secuencia de registros, uno por cada acción (mía y del bot), cada uno con el estado completo de la
partida en ese momento, el actor (humano/bot) y la acción tomada.

**Acceptance Scenarios**:

1. **Given** una partida en curso, **When** el jugador humano juega una carta, **Then** se persiste
   un registro con el estado completo de la partida en ese momento, el asiento del jugador, el tipo
   de acción (jugar carta) y la carta jugada.
2. **Given** una partida en curso, **When** el bot canta truco, **Then** se persiste un registro
   equivalente marcado como acción del bot, sin necesidad de instrumentación específica del bot.
3. **Given** una secuencia de acciones en una partida, **When** se consultan los registros de esa
   partida, **Then** aparecen en orden, sin huecos ni duplicados, cubriendo todas las acciones que
   cambiaron el estado de la partida.
4. **Given** que se está registrando, **When** ocurre cualquier acción jugable, **Then** la lógica
   del juego y los tiempos de respuesta percibidos por el jugador no se ven alterados.
5. **Given** que una acción jugable es rechazada por inválida (no cambia el estado de la partida),
   **When** se procesa, **Then** **no** se persiste ningún registro para ese intento.

---

### User Story 2 - Exportar el historial recopilado para análisis (Priority: P2)

Como dueño del producto, quiero poder extraer todo lo recopilado en un formato consumible fuera del
sistema, para poder analizarlo (manualmente o pasándoselo a un asistente) y decidir qué hacer con
los datos.

**Why this priority**: Los datos recopilados solo generan valor cuando se pueden sacar y mirar. Es
el segundo paso natural una vez que la captura funciona, pero el sistema ya aporta valor aunque la
exportación llegue después (los datos se siguen acumulando).

**Independent Test**: Con varias partidas ya registradas, ejecutar la exportación y verificar que se
obtiene un archivo con una entrada por cada decisión registrada, conteniendo el estado, la acción y
el actor, listo para ser procesado offline.

**Acceptance Scenarios**:

1. **Given** partidas registradas, **When** se solicita la exportación, **Then** se obtiene un
   volcado con una entrada por decisión, cada una autocontenida (estado + acción + actor +
   metadatos de partida).
2. **Given** la exportación, **When** se inspecciona una entrada, **Then** contiene suficiente
   contexto para reconstruir qué se veía y qué se decidió, sin necesidad de consultar el estado vivo
   de la partida.
3. **Given** que el acceso a estos datos es sensible (incluye el estado completo, sin ocultar
   manos), **When** alguien solicita la exportación, **Then** la operación está restringida a un rol
   administrativo/operador y no es accesible para jugadores comunes.

---

### User Story 3 - Garantizar integridad y durabilidad perpetua del registro (Priority: P3)

Como dueño del producto, quiero que el historial sea inmutable, versionado y conservado para
siempre, para que el dataset sea confiable a lo largo del tiempo aunque las reglas o el modelo del
juego cambien en el futuro.

**Why this priority**: Protege el valor a largo plazo del dataset. No es necesario para empezar a
recopilar, pero asegura que lo recopilado siga siendo interpretable y confiable en el futuro.

**Independent Test**: Verificar que los registros existentes nunca se modifican ni se borran al
avanzar la partida o futuras partidas, y que cada registro indica bajo qué versión de esquema fue
capturado.

**Acceptance Scenarios**:

1. **Given** un registro ya escrito, **When** la partida avanza o termina, **Then** ese registro no
   se modifica ni se elimina (solo se agregan nuevos registros).
2. **Given** un cambio futuro en las reglas o en la forma de representar el estado, **When** se
   inspeccionan registros viejos y nuevos, **Then** cada uno declara la versión de esquema con la
   que fue capturado, permitiendo interpretarlos correctamente.
3. **Given** el volumen de datos esperado (un único jugador en producción), **When** se acumulan
   partidas a lo largo del tiempo, **Then** el registro se conserva sin políticas de borrado ni
   expiración.

---

### Edge Cases

- **Partida que no llega a jugarse**: si una partida se cancela en lobby o termina sin acciones
  jugables, no debe generar registros de decisión (no hubo decisiones).
- **Terminación anómala** (abandono, timeout/forfeit por inactividad, cancelación): las acciones
  jugables ocurridas antes de la terminación quedan registradas; la terminación en sí no requiere
  registrar una "decisión".
- **Turno del bot asíncrono**: las decisiones del bot ocurren fuera del hilo de la petición del
  jugador; deben registrarse igual y con el mismo nivel de detalle que las del humano.
- **Partidas concurrentes**: los registros de distintas partidas no deben mezclarse; cada registro
  identifica inequívocamente su partida.
- **Reconexión / reenvío**: una reconexión del jugador que no produce un cambio de estado de la
  partida no debe generar registros.
- **Fallo al registrar**: si la persistencia del registro falla, no debe afectar la jugada en sí
  (la partida sigue su curso); la pérdida de un registro es preferible a degradar la experiencia de
  juego.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST registrar, por cada acción jugable que cambia el estado de una partida
  (jugar carta, cantar truco, responder truco, cantar envido, responder envido, irse al mazo), una
  entrada en un historial persistente.
- **FR-002**: Cada entrada MUST incluir el **estado completo y sin ocultamientos** de la partida
  correspondiente a esa transición (incluyendo información que normalmente se oculta a cada jugador,
  como ambas manos), capturado del lado del servidor y nunca expuesto al cliente durante el juego.
- **FR-003**: Cada entrada MUST identificar al **actor** de la acción distinguiendo si fue un
  jugador humano o un bot.
- **FR-004**: El sistema MUST registrar de la misma forma las decisiones de los bots y las de los
  humanos, sin requerir un mecanismo de captura específico para los bots.
- **FR-005**: Cada entrada MUST registrar la **acción concreta** tomada (p. ej. qué carta, qué
  canto, qué respuesta) y datos suficientes para ubicarla dentro de la partida (identificador de
  partida, número de juego/mano dentro de la serie y posición en la secuencia de la partida).
- **FR-006**: Las entradas de una misma partida MUST poder ordenarse de forma determinística y sin
  ambigüedad, de modo que la secuencia reconstruida no tenga huecos ni duplicados.
- **FR-007**: El historial MUST ser **append-only**: una entrada, una vez escrita, no se modifica ni
  se elimina por el avance de la partida ni por partidas posteriores.
- **FR-008**: Cada entrada MUST indicar la **versión de esquema** bajo la cual fue capturada, para
  que el dataset siga siendo interpretable ante cambios futuros de reglas o de representación.
- **FR-009**: El registro MUST NO alterar la lógica del juego ni los resultados de las partidas, y
  MUST NO degradar de forma perceptible la latencia de las acciones del jugador.
- **FR-010**: Un fallo en el registro de una entrada MUST NO interrumpir ni revertir la jugada;
  el juego continúa normalmente.
- **FR-011**: El sistema MUST permitir **exportar** el historial recopilado en un formato consumible
  fuera del sistema, con una entrada por decisión, autocontenida (estado + acción + actor +
  metadatos).
- **FR-012**: La exportación del historial MUST estar restringida a un rol administrativo/operador y
  MUST NO ser accesible para jugadores comunes.
- **FR-013**: Los intentos de acción **rechazados** (que no producen un cambio de estado en la
  partida) MUST NO generar entradas en el historial.
- **FR-014**: El historial MUST conservarse de forma perpetua, sin políticas automáticas de borrado
  o expiración en esta iteración.

### Key Entities *(include if feature involves data)*

- **Registro de decisión (entrada del historial)**: representa una decisión jugable ocurrida en una
  partida. Atributos clave: identificador de la partida, posición/secuencia dentro de la partida,
  número de juego y de mano, asiento del actor, tipo de actor (humano/bot), tipo de acción, detalle
  de la acción, estado completo de la partida en ese momento, instante de ocurrencia y versión de
  esquema. Es inmutable una vez creado.
- **Estado de partida capturado**: la fotografía completa y sin ocultamientos del estado de la
  partida asociada a una transición, reutilizando la representación de estado que el sistema ya
  conoce para esa partida. Sirve como "lo que se veía/había" en el momento de la decisión.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las acciones jugables que cambian el estado de una partida (humanas y de
  bot) quedan registradas con su estado y su acción.
- **SC-002**: Para cualquier partida registrada, su secuencia de decisiones puede reconstruirse
  completa, en orden y sin huecos ni duplicados.
- **SC-003**: Activar el registro no produce cambios observables en el desarrollo ni en el resultado
  de las partidas, ni un aumento perceptible (para el jugador) en el tiempo de respuesta de las
  acciones.
- **SC-004**: Es posible exportar todo lo recopilado a un archivo consumible offline en una sola
  operación, obteniendo una entrada por decisión registrada.
- **SC-005**: Ningún registro escrito es modificado ni eliminado posteriormente (verificable a lo
  largo de múltiples partidas).
- **SC-006**: Cada entrada exportada es autocontenida: permite identificar la partida, el actor, la
  acción y el estado del momento sin consultar ninguna otra fuente.

## Assumptions

- **Alcance de partidas**: se registran todas las partidas que llegan a tener acciones jugables, sin
  importar su modalidad (casual, rápida, liga, copa, campaña), porque todas atraviesan el mismo
  flujo de acciones. No se distingue por tipo de partida en esta iteración.
- **Único jugador en producción**: el volumen de datos es bajo (un solo jugador humano real), por lo
  que no se contemplan en esta iteración requisitos de alto rendimiento, particionado ni archivado
  del historial.
- **Privacidad**: dado que el único jugador humano es el propio dueño del producto, capturar el
  estado completo sin ocultar manos no plantea un problema de privacidad de terceros en esta
  iteración.
- **Sin uso analítico todavía**: esta iteración solo recopila. El análisis, las comparaciones
  humano-vs-bot y cualquier ajuste o entrenamiento de bots quedan fuera de alcance y se decidirán
  más adelante con los datos en mano.
- **Durabilidad sobre completitud puntual**: ante un fallo de registro, se prioriza no afectar la
  partida; perder ocasionalmente una entrada es aceptable frente a degradar el juego.
- **Captura del estado de la decisión vía snapshot del momento**: se persiste la fotografía del
  estado resultante de cada transición; el par "estado previo + acción siguiente" se deriva de la
  secuencia al analizar/exportar.
