# Feature Specification: Catálogo de Logros

**Feature Branch**: `007-achievements-catalog`

**Created**: 2026-06-01

**Status**: Draft

**Input**: User description: "Exponer un catálogo de logros para que el frontend sepa qué logros
existen, sin depender de hardcodear la lista de códigos. El perfil sigue devolviendo solo los
logros desbloqueados; el frontend combina catálogo + perfil para mostrar la grilla completa con
una marca de desbloqueado. El título y la descripción de cada logro los resuelve el frontend a
partir del código. No se implementan logros ocultos."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver qué logros existen en el juego (Priority: P1)

Como jugador, quiero ver la lista completa de logros que existen en el juego, para saber qué
desafíos puedo perseguir aunque todavía no haya desbloqueado ninguno.

**Why this priority**: Es el objetivo central de la feature. Hoy el jugador no tiene forma de saber
qué logros existen ni cuántos hay; solo ve los que ya consiguió. Sin esta capacidad, el resto no
aporta valor.

**Independent Test**: Se puede probar de forma independiente consultando el catálogo de logros y
verificando que devuelve la lista completa de logros existentes, incluso para un jugador nuevo que
no desbloqueó ninguno.

**Acceptance Scenarios**:

1. **Given** un jugador autenticado que no desbloqueó ningún logro, **When** consulta el catálogo de
   logros, **Then** recibe la lista completa de todos los logros existentes en el juego.
2. **Given** que existen N logros definidos en el juego, **When** se consulta el catálogo, **Then**
   la respuesta contiene exactamente esos N logros, sin omisiones ni duplicados.

---

### User Story 2 - Ver mi progreso de logros (Priority: P2)

Como jugador, quiero ver cuáles de los logros existentes ya desbloqueé y cuáles me faltan, para
medir mi progreso.

**Why this priority**: Aporta el valor de "progreso", pero depende de que primero exista el catálogo
(P1). La combinación entre catálogo y logros desbloqueados es responsabilidad de la capa de
presentación, que cruza ambos conjuntos por el identificador de cada logro.

**Independent Test**: Se puede probar consultando catálogo y perfil de un jugador con algunos logros
desbloqueados, y verificando que cada logro del catálogo puede marcarse como desbloqueado o
pendiente según corresponda.

**Acceptance Scenarios**:

1. **Given** un jugador que desbloqueó algunos logros, **When** se cruza el catálogo con sus logros
   desbloqueados, **Then** cada logro del catálogo queda identificado como desbloqueado o pendiente.
2. **Given** un logro desbloqueado, **When** se consulta el perfil del jugador, **Then** se incluye
   la información de cuándo y en qué partida se desbloqueó.

---

### Edge Cases

- ¿Qué pasa cuando se agrega un logro nuevo al juego? El catálogo debe reflejarlo automáticamente
  sin requerir cambios en el almacenamiento de datos de los jugadores existentes.
- ¿Qué pasa si un jugador tiene un logro desbloqueado cuyo identificador ya no existe en el catálogo
  vigente? Este caso se considera fuera de alcance: los identificadores de logros no se eliminan ni
  renombran una vez publicados.
- ¿Qué pasa cuando el jugador no desbloqueó ningún logro? El catálogo se devuelve igual (completo) y
  el conjunto de desbloqueados queda vacío.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST exponer un catálogo con la lista completa de los logros existentes en
  el juego.
- **FR-002**: El catálogo MUST identificar cada logro mediante su código estable, que es el mismo
  identificador usado para reportar logros desbloqueados.
- **FR-003**: El catálogo MUST ser idéntico para todos los jugadores e independiente de su progreso
  (no depende de quién lo consulta).
- **FR-004**: El sistema MUST mantener el catálogo como única fuente de verdad sobre qué logros
  existen, de modo que al agregar un logro nuevo aparezca en el catálogo sin pasos manuales
  adicionales.
- **FR-005**: El sistema MUST seguir devolviendo en el perfil del jugador únicamente los logros que
  ese jugador desbloqueó, junto con el detalle de desbloqueo (momento, partida y número de juego).
- **FR-006**: El sistema MUST NOT incluir título ni descripción de los logros en el catálogo; esos
  textos son resueltos por la capa de presentación a partir del código del logro.
- **FR-007**: El sistema MUST NOT implementar logros ocultos: todos los logros del catálogo son
  visibles para cualquier jugador.
- **FR-008**: El sistema MUST garantizar, mediante una verificación automatizada, que el conjunto de
  códigos de logros publicados se mantenga sincronizado con la documentación de contratos expuesta
  al frontend, de forma que agregar un código nuevo sin documentarlo sea detectado.

### Key Entities *(include if feature involves data)*

- **Logro (definición de catálogo)**: Representa un logro existente en el juego. Su atributo
  esencial es el código estable que lo identifica. No incluye textos de presentación.
- **Logro desbloqueado**: Representa la consecución de un logro por parte de un jugador. Se
  relaciona con la definición de catálogo a través del código, y agrega el momento de desbloqueo, la
  partida y el número de juego en que ocurrió.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un jugador nuevo, sin logros desbloqueados, puede ver el 100% de los logros existentes
  en el juego.
- **SC-002**: El frontend puede construir la vista "todos los logros con marca de desbloqueado" sin
  mantener una lista de logros codificada a mano, eliminando esa duplicación.
- **SC-003**: Al agregar un logro nuevo al juego, aparece en el catálogo sin cambios adicionales en
  el almacenamiento ni en otros recursos del sistema.
- **SC-004**: Si se agrega un código de logro nuevo sin actualizar la documentación de contratos, la
  verificación automatizada falla, evitando que la desincronización llegue a producción.

## Assumptions

- El catálogo de logros es información pública del juego (no revela datos sensibles del jugador) y
  puede consultarse por cualquier jugador autenticado.
- El frontend es responsable de mapear cada código de logro a su título y descripción, así como de
  la internacionalización de esos textos.
- Los códigos de logros son estables: una vez publicados no se eliminan ni renombran, por lo que no
  se contempla migración de logros desbloqueados históricos.
- Se reutiliza el recurso de perfil de jugador existente para los logros desbloqueados; esta feature
  no cambia su forma actual.
- El conjunto de logros existentes hoy es el definido en el dominio del juego; esta feature expone
  ese conjunto, no agrega ni quita logros.
