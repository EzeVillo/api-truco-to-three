<!--
SYNC IMPACT REPORT
==================
Version change: [unversioned template] → 1.0.0
Modified principles: N/A (primera versión real)
Added sections:
  - I. Arquitectura Hexagonal + DDD
  - II. Dominio Puro
  - III. Test-First con Coverage Mínimo
  - IV. Español como Idioma de Trabajo
  - V. Simplicidad / YAGNI
  - Restricciones Técnicas
  - Flujo de Desarrollo
  - Governance
Removed sections: todos los placeholders del template
Templates actualizados:
  - ⚠ .specify/templates/spec-template.md — INTENCIONAL: dejado en inglés. Los templates son
    archivos del paquete Spec Kit y se sobreescriben con actualizaciones. La directiva de idioma
    se aplica vía CLAUDE.md (siempre en contexto) y via el Principio IV de esta constitution.
  - ⚠ .specify/templates/plan-template.md — mismo criterio.
  - ⚠ .specify/templates/tasks-template.md — mismo criterio.
  - ✅ CLAUDE.md — directiva de idioma añadida en sección "Idioma de Trabajo (Spec Kit)".
Follow-up TODOs:
  - Ninguno. No quedan bracket tokens sin resolver.
-->

# api-truco-to-three — Constitution

## Principios Fundamentales

### I. Arquitectura Hexagonal + DDD (NO NEGOCIABLE)

El proyecto DEBE respetar la arquitectura Clean/Hexagonal con DDD, reforzada en build time por
ArchUnit. La regla de dependencia es estricta y unidireccional:

```
Domain → Application → Infrastructure
```

- **Domain**: Java puro, cero Spring. Contiene agregados, value objects, eventos de dominio e
  interfaces de puertos de salida.
- **Application**: Orquesta casos de uso. Contiene handlers de commands/queries, interfaces de
  puertos de entrada, DTO assemblers y handlers de eventos de aplicación. Sin imports de Spring
  framework.
- **Infrastructure**: Spring Boot, JPA, REST controllers, WebSocket, seguridad y repositorios.

Los controllers HTTP DEBEN depender de interfaces de puertos de entrada, nunca de implementaciones
directas. Los agregados del dominio NO deben importarse entre sí; la comunicación cruzada va
por eventos de dominio.

**Rationale**: Separación de concerns, testabilidad del dominio sin infraestructura, y
mantenibilidad a largo plazo. ArchUnit romperá el build si se viola esta regla.

### II. Dominio Puro

El paquete `com.villo.truco.domain` DEBE permanecer libre de cualquier dependencia de frameworks
(Spring, JPA, Hibernate, etc.). Solo se permiten dependencias de Java estándar.

- Los agregados y value objects son POJOs.
- La lógica de negocio vive en el dominio, no en handlers de aplicación ni en servicios de
  infraestructura.
- Las interfaces de repositorios se definen en domain; las implementaciones en infrastructure.

**Rationale**: El dominio puro garantiza que la lógica de negocio sea testeable de forma aislada,
sin necesidad de contexto Spring ni base de datos.

### III. Test-First con Coverage Mínimo

Todo código nuevo DEBE estar acompañado de tests. JaCoCo verifica un mínimo del 70% de cobertura
de líneas en el build.

- Los tests de dominio NO deben depender de Spring ni de Docker (usan H2 en memoria).
- Flyway está deshabilitado en tests; el DDL es `create-drop`.
- Los tests de arquitectura (`CleanArchitectureTest`) son parte del suite y fallarán si se viola
  el layering.
- Los títulos de los tests DEBEN estar escritos en español y describir claramente el escenario.

**Rationale**: La cobertura mínima previene regresiones. Los títulos en español mantienen
consistencia con el idioma de trabajo del proyecto.

### IV. Español como Idioma de Trabajo (NO NEGOCIABLE)

Todo artefacto generado por el flujo Spec Kit DEBE estar escrito en español:

- Especificaciones (`spec.md`)
- Planes de implementación (`plan.md`)
- Listas de tareas (`tasks.md`)
- Checklists
- Documentación de modelos de datos, contratos y research
- Títulos y descripciones de tests generados o propuestos

El código fuente (nombres de clases, métodos, variables) puede permanecer en inglés siguiendo
las convenciones existentes del proyecto. Los comentarios en código son opcionales, pero si se
escriben, DEBEN estar en español.

**Rationale**: El equipo opera en español. Los artefactos de diseño en español reducen la
fricción de lectura y revisión, y mantienen consistencia con la documentación del proyecto
(`README.md`, `docs/CONTRATOS_API.md`).

### V. Simplicidad / YAGNI

No se deben agregar abstracciones, patrones o features más allá de lo que la tarea requiere.

- Tres líneas similares son preferibles a una abstracción prematura.
- No se implementan casos de error que no pueden ocurrir.
- No se diseña para requerimientos hipotéticos futuros.
- Las implementaciones a medias no se consideran completas.

**Rationale**: La complejidad innecesaria aumenta el costo de mantenimiento y viola el principio
de dominio puro cuando se filtra hacia las capas incorrectas.

## Restricciones Técnicas

- **Stack**: Java + Spring Boot, PostgreSQL (producción), H2 PostgreSQL-mode (tests).
- **Comunicación en tiempo real**: WebSocket/STOMP. Rutas de usuario: `/user/queue/*`.
  Tópicos públicos: `/topic/public-*-lobby`.
- **Seguridad**: JWT para autenticación. La clave se configura vía `TRUCO_JWT_SECRET`.
- **Documentación de API**: Swagger disponible en `/swagger-ui/index.html` cuando corre localmente.
- **Coverage**: Mínimo 70% de líneas. Configurable vía propiedad Gradle `coverageMinimum`.
- **Documentación a actualizar**: Cada cambio que afecte un recurso REST, una capacidad del
  sistema, un bounded context, enums, tablas o reglas de negocio DEBE reflejarse en `README.md`
  y/o `docs/CONTRATOS_API.md`.

## Flujo de Desarrollo

1. Crear rama de feature con `/speckit-git-feature`.
2. Especificar con `/speckit-specify` (genera `spec.md` en español).
3. Clarificar con `/speckit-clarify` si hay puntos ambiguos.
4. Planificar con `/speckit-plan` (genera `plan.md` en español).
5. Generar tareas con `/speckit-tasks` (genera `tasks.md` en español).
6. Implementar con `/speckit-implement`.
7. Revisar el plan antes de cerrar: verificar si `README.md` y `docs/CONTRATOS_API.md` necesitan
   actualizarse.

Cada comando puede auto-commitear vía hooks de git configurados en `.specify/extensions.yml`.

## Governance

Esta constitution rige sobre cualquier otra práctica o convención del proyecto. Las enmiendas
requieren:

1. Documentar el cambio propuesto.
2. Justificar por qué el principio actual es insuficiente o incorrecto.
3. Actualizar esta constitution y propagar el cambio a los templates dependientes.
4. Incrementar la versión según semver:
    - MAJOR: eliminación o redefinición incompatible de un principio.
    - MINOR: nuevo principio o sección con guía sustancial.
    - PATCH: clarificaciones, correcciones de redacción.

Todos los PRs/reviews DEBEN verificar cumplimiento con los principios I, II y IV como mínimo.

**Versión**: 1.0.0 | **Ratificado**: 2026-05-22 | **Última enmienda**: 2026-05-22
