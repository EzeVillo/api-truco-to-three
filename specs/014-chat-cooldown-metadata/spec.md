# Feature Specification: Estado de envio en chat

**Feature Branch**: `014-chat-cooldown-metadata`

**Created**: 2026-06-08

**Status**: Draft

**Input**: User description: "Exponer al frontend si el jugador autenticado puede enviar mensajes en
el chat y, cuando no puede por cooldown, desde cuando puede volver a enviar. El estado debe estar
disponible al obtener el chat despues de un refresh, no solo como resultado de intentar enviar."

## Clarifications

### Session 2026-06-08

- Q: Donde debe exponerse el estado de cooldown del remitente? -> A: En el estado del chat obtenido
  por lectura y en la confirmacion de envio; no es necesario incluirlo en el error de rate limit.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Saber si puedo escribir al abrir un chat (Priority: P1)

Como jugador que participa en un chat, quiero que la aplicacion sepa al cargar el chat si puedo
mandar un mensaje ahora o si todavia estoy en cooldown, para que despues de refrescar la pantalla el
campo de escritura refleje el estado real sin esperar a que falle un envio.

**Why this priority**: Es el flujo principal de reconciliacion. Sin este dato en la lectura del
chat,
la aplicacion pierde el estado de cooldown al refrescar y solo lo recupera provocando un error.

**Independent Test**: Se puede probar enviando un mensaje, refrescando la aplicacion antes de que
pasen 2 segundos y verificando que la lectura del chat informa que el jugador todavia no puede
enviar.

**Acceptance Scenarios**:

1. **Given** un jugador participante de un chat sin cooldown activo, **When** obtiene el estado del
   chat, **Then** el sistema informa que puede enviar mensajes ahora.
2. **Given** un jugador que envio un mensaje hace menos de 2 segundos, **When** obtiene el estado
   del
   chat despues de refrescar, **Then** el sistema informa que no puede enviar todavia y desde cuando
   podra volver a enviar.

---

### User Story 2 - Ver cuando puedo volver a escribir tras enviar (Priority: P1)

Como jugador que participa en un chat, quiero que la aplicacion sepa exactamente cuando puedo volver
a mandar un mensaje despues de un envio exitoso, para que el campo de escritura y el boton de envio
puedan mostrar un cooldown coherente sin depender de calculos ambiguos.

**Why this priority**: Evita que el jugador intente enviar mensajes que el sistema ya sabe que
todavia no aceptara y mantiene la UI sincronizada sin una lectura adicional inmediata.

**Independent Test**: Se puede probar enviando un mensaje valido en cualquier chat disponible y
verificando que la confirmacion del sistema incluye el estado de envio actualizado para ese jugador.

**Acceptance Scenarios**:

1. **Given** un jugador participante de un chat, **When** envia un mensaje valido, **Then** el
   sistema
   confirma el envio e informa que el jugador no puede enviar otro mensaje hasta que termine el
   cooldown.
2. **Given** un jugador que acaba de enviar un mensaje, **When** la aplicacion recibe el estado de
   envio actualizado, **Then** puede deshabilitar o ajustar la accion de envio hasta ese momento.

---

### User Story 3 - Entender un bloqueo por rate limit (Priority: P2)

Como jugador que intenta mandar mensajes demasiado rapido, quiero que la aplicacion reciba un motivo
distinguible de rechazo, para que pueda explicarme que el intento fue bloqueado por cooldown sin
confundirlo con otros errores.

**Why this priority**: El bloqueo por rate limit ya existe; el frontend debe poder distinguirlo,
pero
no necesita que ese error sea la fuente principal del estado de cooldown.

**Independent Test**: Se puede probar enviando dos mensajes del mismo jugador con menos de 2
segundos
de diferencia y verificando que el rechazo tiene un motivo estable de rate limit.

**Acceptance Scenarios**:

1. **Given** un jugador que envio un mensaje hace menos de 2 segundos, **When** intenta enviar otro,
   **Then** el sistema rechaza el intento por rate limit con un motivo distinguible.
2. **Given** un intento rechazado por rate limit, **When** la aplicacion necesita reconciliar el
   estado, **Then** puede obtener el estado del chat para saber si el jugador ya puede enviar.

---

### User Story 4 - Mantener los chats entre participantes simples (Priority: P3)

Como participante que recibe mensajes de otras personas, quiero seguir viendo los mensajes en tiempo
real sin recibir actualizaciones de cooldown ajenas innecesarias, para que el chat no genere ruido
de
estado que no afecta mis acciones.

**Why this priority**: El cooldown solo limita al remitente. Exponerlo a todos los participantes no
aporta valor directo y aumenta la cantidad de informacion que las interfaces deben ignorar.

**Independent Test**: Se puede probar observando los eventos de chat al enviar mensajes y
verificando
que los participantes reciben el mensaje, pero no reciben una cuenta regresiva o cambios periodicos
de
cooldown del remitente.

**Acceptance Scenarios**:

1. **Given** un mensaje enviado correctamente, **When** otros participantes reciben la novedad del
   chat, **Then** ven el contenido y los datos del mensaje sin necesitar metadata de cooldown del
   remitente.
2. **Given** un jugador bloqueado por rate limit, **When** el bloqueo afecta solo a ese jugador,
   **Then** no se emiten actualizaciones periodicas a otros participantes por ese cooldown.

### Edge Cases

- Si el reloj del cliente esta desfasado, la aplicacion debe poder usar el instante absoluto
  informado por el sistema como fuente de verdad y recalcular el tiempo restante localmente.
- Si el tiempo restante ya llego a cero cuando la respuesta llega al cliente, la aplicacion debe
  poder reactivar el envio sin esperar un evento adicional.
- Si el intento falla por mensaje vacio, longitud maxima, falta de permisos o chat inexistente, el
  error no debe confundirse con rate limit ni incluir metadata obligatoria de cooldown.
- Si el jugador refresca la aplicacion justo despues de enviar un mensaje, la lectura del chat debe
  reconstruir el estado de envio aunque la confirmacion original ya no este en memoria.
- Si el primer mensaje crea el chat de forma diferida, la confirmacion debe incluir tanto el
  identificador necesario para seguir conversando como el estado de envio actualizado.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST informar, al obtener el estado de un chat, si el jugador autenticado
  puede enviar un mensaje en ese momento.
- **FR-002**: Cuando el jugador autenticado no pueda enviar por cooldown, el estado del chat MUST
  informar `nextMessageAllowedAt` como el primer instante en el que ese jugador puede enviar otro
  mensaje sin violar el cooldown vigente, representado como milisegundos epoch.
- **FR-003**: Cuando un envio de mensaje sea rechazado por rate limit, el sistema MUST devolver un
  codigo de error estable y distinguible para ese motivo.
- **FR-004**: El error por rate limit MUST NOT incluir `nextMessageAllowedAt` ni `retryAfterMs`; la
  fuente de verdad para reconciliar el cooldown debe ser el estado del chat.
- **FR-005**: Despues de aceptar un mensaje, la confirmacion MUST incluir el estado de envio
  actualizado para el jugador que envio el mensaje.
- **FR-006**: Los rechazos por otros motivos de chat MUST conservar su motivo especifico y no deben
  ser reportados como rate limit.
- **FR-007**: La confirmacion del primer mensaje de un chat creado de forma diferida MUST incluir el
  identificador del chat y el estado de envio actualizado.
- **FR-008**: La funcionalidad MUST mantener la regla actual de cooldown minimo de 2 segundos entre
  mensajes del mismo jugador.
- **FR-009**: El sistema MUST evitar actualizaciones periodicas o cuentas regresivas en tiempo real
  para el cooldown de chat; la aplicacion cliente debe poder derivar la cuenta regresiva desde los
  campos informados.
- **FR-010**: La documentacion publica del contrato de chat MUST describir los nuevos campos, su
  formato, cuando aparecen en lecturas y confirmaciones de envio, y como distinguir el bloqueo por
  rate limit.

### Key Entities

- **Intento de envio de mensaje**: Accion de un jugador participante que intenta publicar contenido
  en un chat. Sus resultados posibles incluyen aceptacion, rechazo por rate limit u otros rechazos
  de
  validacion/permisos.
- **Cooldown de chat**: Ventana temporal por jugador que impide enviar otro mensaje antes de que
  transcurra el minimo permitido.
- **Estado de envio del jugador autenticado**: Datos incluidos en el estado del chat que indican si
  el jugador puede enviar ahora y, cuando no puede por cooldown, desde cuando puede volver a enviar.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En el 100% de las lecturas exitosas de un chat, la aplicacion cliente recibe si el
  jugador autenticado puede enviar mensajes en ese momento.
- **SC-002**: En el 100% de los estados donde el jugador autenticado esta en cooldown, la aplicacion
  cliente recibe el instante de proximo envio permitido sin interpretar texto libre.
- **SC-003**: El jugador ve una indicacion de reintento disponible con una desviacion maxima de 250
  ms
  respecto del cooldown informado, bajo condiciones normales de red.
- **SC-004**: La incorporacion de la metadata no agrega actualizaciones periodicas de chat ni cambia
  la visibilidad de mensajes para otros participantes.
- **SC-005**: La documentacion del contrato permite a un desarrollador de frontend implementar el
  cooldown sin preguntar reglas adicionales sobre formato, unidades o casos de error.

## Assumptions

- El cooldown vigente de chat continua siendo de 2 segundos entre mensajes del mismo jugador.
- El cliente mostrara la cuenta regresiva localmente a partir de `nextMessageAllowedAt` y no
  requerira
  eventos periodicos del sistema.
- `nextMessageAllowedAt` se expresa en milisegundos epoch para mantener consistencia con otros
  timestamps expuestos al frontend.
- El error por rate limit puede seguir siendo un error de negocio distinguible, pero no es la fuente
  principal del estado de cooldown para el frontend.
- La feature no cambia quien puede enviar o leer mensajes, el tamano maximo de mensaje ni el limite
  de mensajes retenidos por chat.
