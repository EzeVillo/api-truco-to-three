# Clean Code Playbook (Reusable + AI-Friendly)

Este documento esta pensado para dos usos:

- Guia humana para decidir **que practica aplicar segun la situacion**.
- Instruccion operativa para que un **agente de IA** la aplique en cualquier proyecto.

Si buscas una version corta y ejecutable para agentes, usar primero `AGENTS.md`.

No depende de Truco Master. Incluye una seccion final de adaptacion al repo actual como ejemplo.

## 1. Proposito

- Reducir deuda tecnica sin frenar entrega.
- Mantener cambios predecibles y testeables.
- Separar claramente negocio, aplicacion e infraestructura.
- Evitar regresiones por reglas duplicadas o dispersas.

## 2. Principios No Negociables

- Una responsabilidad principal por clase/modulo.
- Dependencias apuntan hacia adentro (el negocio no depende del framework).
- Fail-fast en bordes y en invariantes.
- Contratos explicitos entre capas (DTO/Command/Port).
- Side effects controlados y en orden estable.
- Todo bug de negocio deja test de regresion.

## 3. Matriz Universal: Situacion -> Practica -> Resultado

| Situacion                                  | Practica recomendada                                               | Resultado esperado                       |
|--------------------------------------------|--------------------------------------------------------------------|------------------------------------------|
| Entra request externo (HTTP, MQ, CLI)      | Adaptador delgado + validacion basica + delegacion a caso de uso   | Borde simple y sin logica de negocio     |
| Llegan primitivas con semantica de dominio | Convertir a Value Objects en command/query/factory                 | Tipos seguros, menos errores por strings |
| Regla de negocio con muchas condiciones    | Policy/Specification/State Machine                                 | Regla centralizada y testeable           |
| Operacion modifica estado + notifica       | Flujo fijo: cargar -> ejecutar -> persistir -> publicar -> limpiar | Efectos laterales predecibles            |
| Error funcional esperado                   | Excepcion especifica + mapeo uniforme de error                     | API consistente para cliente             |
| Operacion con concurrencia                 | Lock por recurso + idempotencia + test concurrente                 | Sin carreras ni duplicados               |
| Proyecto crece por equipos                 | Test de arquitectura + checklist de PR                             | Consistencia de capas en el tiempo       |

## 4. Reglas Operativas Por Capa (Template)

### 4.1 Domain

Usar cuando:

- Hay reglas invariantes o decisiones de negocio.

Reglas:

- No depende de framework.
- No conoce transporte (HTTP/WS/DB).
- Expresa reglas con lenguaje de negocio.
- Lanza excepciones de dominio ante invalidez.
- Emite eventos de dominio cuando ocurren hechos relevantes.

### 4.2 Application

Usar cuando:

- Hay que orquestar una accion de negocio completa.

Reglas:

- Casos de uso pequenos, orientados a una accion.
- Sin detalles de framework.
- Depende de puertos (interfaces), no de adaptadores concretos.
- Mantiene orden estable de side effects.

Orden recomendado:

1. Resolver estado/agregado.
2. Ejecutar metodo de dominio.
3. Persistir cambios.
4. Publicar eventos.
5. Limpiar eventos locales.

### 4.3 Infrastructure

Usar cuando:

- Hay integracion con framework/protocolo/DB/broker.

Reglas:

- Controllers/handlers de transporte delgados.
- Mapeo DTO <-> Command/Query/Response.
- Security, transacciones y wiring viven aqui.
- Nunca duplicar regla de negocio que ya exista en domain.

## 5. Patron De Conversion De Tipos (Muy Recomendado)

Objetivo:

- Evitar que `String/int` ambiguos circulen por todo el sistema.

Regla:

- Convertir en el borde de aplicacion (command/query constructor o factory).

Ejemplo generico:

```java
public record CreateOrderCommand(OrderId orderId, CustomerId customerId, Money total) {

  public CreateOrderCommand(String orderId, String customerId, BigDecimal total) {

    this(OrderId.of(orderId), CustomerId.of(customerId), Money.of(total));
  }

}
```

Beneficio:

- Errores de formato y semantica fallan temprano.

## 6. Manejo De Errores (Contrato Estable)

Reglas:

- Excepciones de dominio para reglas del negocio.
- Excepciones de aplicacion para contexto tecnico/funcional (not found, unauthorized, etc.).
- Un unico mapper de errores por adaptador externo.

Contrato sugerido:

```json
{
  "errorCode": "DOMAIN_RULE_VIOLATION",
  "message": "Human readable message",
  "timestamp": "2026-03-10T12:00:00Z",
  "traceId": "optional"
}
```

## 7. Concurrencia E Idempotencia

Aplicar cuando:

- Dos usuarios/procesos pueden ejecutar la misma accion en paralelo.

Checklist minimo:

- [ ] Lock por identidad de recurso (ej. `orderId`, `matchId`).
- [ ] Operacion idempotente ante reintentos.
- [ ] Test concurrente repetido.
- [ ] Sin duplicacion de eventos/efectos.

## 8. Testing Strategy (Portable)

Piramide recomendada:

- Unit tests de dominio (prioridad maxima).
- Tests de caso de uso (orquestacion).
- Integracion en bordes (security, serializacion, contratos).
- Arquitectura (reglas de dependencias por capa).

Regla de regresion:

- Todo bug corregido agrega test que falle antes y pase despues.

## 9. Checklist Universal Para PR

- [ ] Cambio respeta capas.
- [ ] No se agrego logica de negocio en adapters.
- [ ] Input externo convertido a tipos de dominio donde corresponde.
- [ ] Excepciones son especificas y mapeadas.
- [ ] Hay test de comportamiento para regla nueva/cambiada.
- [ ] Si hay concurrencia, hay test concurrente e idempotencia.
- [ ] Contratos externos actualizados (docs/tests) si cambia entrada/salida.

## 10. Anti-Patrones Universales

- Controllers o handlers de transporte con reglas de negocio.
- Primitivos cruzando multiples capas sin semantica.
- Estado global mutable sin control de concurrencia.
- Reglas duplicadas en backend + frontend sin fuente unica.
- Excepciones genericas para errores de negocio.
- Falta de tests de regresion despues de bugs reales.

## 11. Protocolo Para Agentes IA (Copiable A Otros Repos)

Objetivo:

- Que el agente aplique Clean Code de manera consistente y verificable.

### 11.1 Algoritmo de trabajo

1. Identificar capa afectada (`domain`, `application`, `infrastructure`).
2. Ubicar regla de negocio fuente (si existe) y evitar duplicarla.
3. Aplicar cambio en la capa correcta.
4. Verificar side effects con el orden recomendado.
5. Ejecutar/actualizar tests relevantes.
6. Reportar: archivos tocados, riesgo residual, cobertura de tests.

### 11.2 Reglas de decision rapida

- Si el cambio altera reglas: tocar primero `domain`.
- Si el cambio altera flujo: tocar `application`.
- Si el cambio altera transporte/framework: tocar `infrastructure`.
- Si no hay test de la regla afectada: crear uno antes de cerrar.

### 11.3 Definicion de terminado

El cambio solo esta terminado si:

- Cumple reglas de capa.
- Tiene tests acordes al impacto.
- Mantiene contrato externo o lo documenta.
- No introduce duplicacion de regla de negocio.
