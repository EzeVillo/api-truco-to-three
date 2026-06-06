# Especificación de Feature: Espectar partidas de amigos

**Rama de feature**: `011-friend-spectate`

**Creado**: 2026-06-06

**Estado**: Borrador

**Entrada**: Descripción del usuario: "Permitir que los amigos puedan espectar partidas activas de
otros amigos, sin posibilidad de deshabilitar que te especteen."

## Clarifications

### Session 2026-06-06

- Q: ¿La feature debe crear un mecanismo nuevo de espectador o apoyarse en las formas de espectar
  que ya existen? -> A: Debe apoyarse en las formas actuales de espectar y ampliar su elegibilidad
  para incluir amigos confirmados.

## Escenarios de Usuario y Pruebas *(obligatorio)*

### Historia de Usuario 1 - Entrar a espectar una partida activa de un amigo (Prioridad: P1)

Como usuario autenticado, quiero ver cuándo un amigo está jugando y entrar como espectador a su
partida activa, para acompañar o seguir la partida en vivo sin participar como jugador.

**Por qué esta prioridad**: Es el flujo central de la feature y entrega valor social inmediato sin
depender de funcionalidades secundarias.

**Prueba independiente**: Puede probarse con dos usuarios que ya son amigos: uno inicia una partida
y el otro accede a la acción de espectar usando la experiencia de espectador existente, entra a una
vista de espectador y ve el estado actual de la partida sin controles de jugador.

**Escenarios de aceptación**:

1. **Dado** un usuario tiene un amigo con una partida activa, **Cuando** abre la lista o superficie
   social donde se muestra ese amigo, **Entonces** ve una acción disponible para espectar la
   partida.
2. **Dado** un usuario selecciona espectar a un amigo que está jugando, **Cuando** la partida sigue
   activa, **Entonces** ingresa a una vista de espectador con marcador, estado visible de mesa y
   avance de la partida.
3. **Dado** un usuario está espectando una partida de un amigo, **Cuando** observa la mesa, *
   *Entonces** no ve controles para cantar, jugar cartas, responder acciones ni modificar el
   resultado.

---

### Historia de Usuario 2 - Restringir el acceso a amigos confirmados (Prioridad: P2)

Como jugador, quiero que solo mis amigos confirmados puedan espectar mi partida, para que la
visibilidad social quede limitada a relaciones aceptadas.

**Por qué esta prioridad**: Define el límite de privacidad mínimo de la feature y evita que usuarios
sin relación social accedan a partidas privadas.

**Prueba independiente**: Puede probarse con tres usuarios: un jugador en partida, un amigo
confirmado y un usuario sin amistad. Solo el amigo confirmado debe poder iniciar la vista de
espectador.

**Escenarios de aceptación**:

1. **Dado** un usuario es amigo confirmado de un jugador en partida, **Cuando** intenta espectar esa
   partida, **Entonces** el sistema permite el ingreso como espectador.
2. **Dado** un usuario no es amigo confirmado del jugador en partida, **Cuando** intenta acceder a
   la partida como espectador, **Entonces** el sistema bloquea el acceso y muestra un mensaje claro
   de falta de permiso.
3. **Dado** una solicitud de amistad está pendiente, rechazada o eliminada, **Cuando** el usuario
   intenta espectar una partida del otro usuario, **Entonces** el acceso no está disponible.

---

### Historia de Usuario 3 - Mantener la experiencia correcta durante y al finalizar la partida (Prioridad: P3)

Como espectador, quiero que la vista refleje cambios importantes de la partida y su finalización,
para entender qué está ocurriendo y salir o volver al área social sin confusión.

**Por qué esta prioridad**: Hace que la experiencia sea completa cuando la partida cambia de estado,
termina o deja de estar disponible.

**Prueba independiente**: Puede probarse entrando como espectador a una partida, avanzando turnos
hasta terminarla y verificando que la vista informa el resultado final y ofrece una salida clara.

**Escenarios de aceptación**:

1. **Dado** un espectador está viendo una partida, **Cuando** cambia el marcador, se juega una carta
   visible o cambia el turno, **Entonces** la vista se actualiza con el nuevo estado visible.
2. **Dado** una partida espectada finaliza, **Cuando** se determina el ganador, **Entonces** el
   espectador ve el resultado final, incluyendo si alguien ganó por llegar exactamente a 3 puntos o
   perdió por pasarse de 3.
3. **Dado** una partida deja de estar disponible antes de que el usuario entre, **Cuando** intenta
   espectarla, **Entonces** recibe un mensaje claro y vuelve a una pantalla desde la que pueda
   seguir navegando.

---

### Casos Límite

- El amigo termina la partida entre el momento en que se muestra la acción de espectar y el momento
  en que el usuario intenta entrar.
- La amistad se elimina o bloquea mientras el usuario está espectando.
- El jugador abandona, se desconecta o la partida se cancela.
- Dos amigos del mismo jugador intentan espectar la misma partida al mismo tiempo.
- El espectador pierde conexión temporalmente y luego vuelve a la vista.
- La partida pertenece a una serie mejor de 1, 3 o 5 y el espectador necesita entender el avance de
  partidas ganadas.
- Una partida está en una etapa donde existe información oculta de juego, como cartas no jugadas o
  decisiones privadas.

## Requisitos *(obligatorio)*

### Requisitos Funcionales

- **FR-001**: El sistema DEBE permitir que un usuario autenticado especte una partida activa de otro
  usuario cuando ambos sean amigos confirmados, además de los casos de espectador ya permitidos por
  el producto.
- **FR-002**: El sistema DEBE mostrar una acción de "Espectar" o equivalente para amigos confirmados
  que estén en una partida activa y disponible para espectadores.
- **FR-003**: El sistema DEBE bloquear el acceso de espectador a usuarios que no sean amigos
  confirmados del jugador cuya partida intentan ver.
- **FR-004**: El sistema DEBE tratar las solicitudes pendientes, rechazadas, eliminadas o bloqueadas
  como relaciones no habilitadas para espectar.
- **FR-005**: El sistema DEBE ofrecer una vista de espectador separada del rol de jugador, sin
  acciones que puedan modificar la partida.
- **FR-006**: El sistema DEBE mostrar al espectador el estado visible de la partida, incluyendo
  marcador, cartas ya reveladas, turno actual cuando corresponda, estado de la mano, estado de la
  partida y avance de serie si aplica.
- **FR-007**: El sistema DEBE ocultar al espectador toda información que no sería públicamente
  visible durante la partida, incluyendo cartas no jugadas de cualquier jugador y decisiones
  privadas pendientes.
- **FR-008**: El sistema DEBE actualizar la vista del espectador cuando ocurran cambios relevantes
  visibles en la partida.
- **FR-009**: El sistema DEBE informar claramente cuando la partida espectada termina, se cancela o
  deja de estar disponible.
- **FR-010**: El sistema DEBE reflejar correctamente la regla de punto exacto de truco-to-three en
  la vista de espectador: una partida individual se gana llegando exactamente a 3 puntos y pasarse
  de 3 puntos hace perder.
- **FR-011**: El sistema DEBE reflejar correctamente el formato de serie cuando corresponda: mejor
  de 1, mejor de 3 o mejor de 5 partidas.
- **FR-012**: El sistema DEBE usar mejor de 3 como formato por defecto cuando una vista de serie
  necesite asumir un formato y no haya una selección explícita.
- **FR-013**: El sistema DEBE mantener habilitada la posibilidad de que amigos confirmados especten
  partidas activas; no debe existir una preferencia, control o ajuste para deshabilitar que amigos
  confirmados especten.
- **FR-014**: El sistema DEBE retirar o invalidar el acceso de espectador si la relación de amistad
  deja de ser válida durante la sesión de espectador.
- **FR-015**: El sistema DEBE mostrar mensajes comprensibles para los casos de acceso denegado,
  partida finalizada, partida no disponible o pérdida de conexión.
- **FR-016**: El sistema DEBE reutilizar la experiencia y reglas generales de espectador existentes
  para registro, permanencia, reconexión, conteo y salida, agregando la amistad confirmada como
  nuevo motivo válido de acceso.

### Entidades Clave

- **Amistad confirmada**: Relación social aceptada entre dos usuarios que habilita la posibilidad de
  espectar partidas activas entre ellos.
- **Partida activa**: Partida individual o dentro de una serie que se encuentra en curso y tiene al
  menos un jugador que es amigo confirmado del espectador.
- **Espectador**: Usuario que observa una partida mediante la experiencia de espectador existente,
  sin participar como jugador y sin capacidad de modificar acciones, cartas, cantos, respuestas o
  puntuación.
- **Vista visible de partida**: Conjunto de datos que el espectador puede ver sin revelar
  información oculta, incluyendo marcador, cartas reveladas, estado general y resultado.
- **Serie**: Conjunto de partidas mejor de 1, 3 o 5, donde el avance debe mostrarse de forma
  entendible para el espectador.

## Criterios de Éxito *(obligatorio)*

### Resultados Medibles

- **SC-001**: El 95% de los usuarios puede entrar a espectar una partida activa de un amigo
  confirmado en menos de 10 segundos desde que ve la acción disponible.
- **SC-002**: El 100% de los intentos de espectar partidas de usuarios sin amistad confirmada queda
  bloqueado con un mensaje claro.
- **SC-003**: El 100% de las sesiones de espectador carece de acciones capaces de modificar la
  partida.
- **SC-004**: El 100% de las vistas de espectador oculta cartas no jugadas y decisiones privadas
  antes de que sean visibles dentro de la partida.
- **SC-005**: Al menos el 90% de los espectadores entiende el resultado final de la partida,
  incluyendo victoria por punto exacto o derrota por pasarse de 3, en pruebas de aceptación.
- **SC-006**: El 95% de los cambios visibles de partida se reflejan para el espectador dentro de los
  2 segundos en condiciones normales de conexión.

## Supuestos

- Los usuarios que usan esta feature ya están autenticados.
- La relación social relevante es la amistad confirmada; seguidores, solicitudes pendientes o
  usuarios bloqueados no habilitan espectador.
- La feature aplica a partidas activas de amigos, no a partidas históricas ni repeticiones.
- La feature amplía la elegibilidad de spectate existente; no reemplaza ni duplica los modos
  actuales para liga, copa u otros contextos ya soportados.
- La experiencia inicial no incluye chat de espectadores ni reacciones sociales dentro de la
  partida.
- La vista de espectador debe priorizar integridad competitiva y no revelar información oculta de
  ningún jugador.
- La posibilidad de ser espectado por amigos confirmados es una regla del producto y no una
  preferencia configurable por usuario.
