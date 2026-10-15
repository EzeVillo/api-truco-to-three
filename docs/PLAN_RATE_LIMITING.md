# Rate limiting externo para Truco

## Diseño

Frontend → Worker `truco-api` en Cloudflare → backend existente en Render.
No se modifica el DNS de DonWeb, la raíz del portfolio ni el Worker de IBIT.
El usuario confirmó que actualmente sólo se usa la web, sin apps nativas instaladas.

El Worker limita solicitudes por IP verificada por Cloudflare, sin confiar en
`X-Forwarded-For`, y conserva REST, SockJS y upgrades WebSocket. Responde `429` con
`Retry-After: 60`, CORS y el contrato `ErrorResponse`; los contadores son por ubicación
de Cloudflare y aproximados, no una cuota global exacta. No limita frames dentro de
una conexión WebSocket; se mantiene el límite de chat propio del dominio.

Políticas iniciales por minuto/IP: general 300, login 10, registro 5, invitados 10,
refresh 60 y apertura de conexiones WebSocket/SockJS 20. Los preflights no consumen
cuota. No se cachean respuestas de API ni se inspeccionan cuerpos o JWT en el Worker.

Render valida una clave aleatoria independiente del JWT en cada solicitud, excepto
GET/HEAD de los probes exactos de salud. La clave sólo vive en secretos de Cloudflare
y Render. El filtro permanece en el binario AOT y se activa en runtime mediante
`TRUCO_GATEWAY_ENABLED`; no usar condiciones de beans que AOT fije al compilar.

## Implementación y verificación

1. Worker, bindings, pruebas unitarias sin red y build seco de Wrangler.
2. Filtro del backend, tests de rechazo directo, secreto válido, CORS, WebSocket y
   JWT; build completo y smoke test del binario nativo en CI.
3. Frontend con nueva URL y copia controlada para `429`; actualizar contrato en
   ambos repos, pruebas, lint y build.
4. Publicar Worker con su secreto; publicar FE; después activar la protección en
   Render junto al mismo secreto y desplegar la imagen verificada.
5. Verificar metadata de despliegues y logs sanitizados; no usar navegador, dev
   server ni pruebas manuales del producto. No contratar planes ni recursos pagos.

## Documentación a actualizar

- `README.md`, `docs/PRODUCCION.md`, `docs/decisiones/produccion.md` y `AGENTS.md`:
  ingreso externo, comandos, políticas, secretos y rollback.
- `docs/contratos/00-convenciones.md` y `01-auth.md`, en backend y frontend:
  `429 RateLimitExceededException`, `Retry-After`, posibles `503` del ingreso y
  rechazo del origen directo. Sin cambios de DTOs de éxito ni eventos de juego.

## Publicación y rollback

La protección del origen se activa al final, cuando el frontend nuevo ya usa el
Worker. No rotar secretos JWT ni credenciales de PostgreSQL. El código del backend
se integra con la revisión requerida por GitHub, sin saltarse la protección.
Para volver temporalmente al acceso directo, desactivar primero el filtro de
ingreso y luego restaurar el frontend anterior; documentar que abre el bypass.

Referencias: [Rate Limiting API](https://developers.cloudflare.com/workers/runtime-apis/bindings/rate-limit/),
[WebSockets](https://developers.cloudflare.com/workers/examples/websockets/).
