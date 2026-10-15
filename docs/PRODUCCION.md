# Producción y migración a ezevillo.com

## Decisión sobre el gateway

Se mantiene un único backend en Render, con REST y WebSocket/STOMP por HTTPS/WSS.
Render ya termina TLS y enruta el tráfico al servicio. Un API gateway propio agregaría
un despliegue y otro punto de fallo sin resolver un requisito actual de enrutamiento.
Se reconsidera cuando existan varios servicios públicos, cuotas por cliente, políticas
compartidas o una necesidad concreta de WAF/rate limiting en el perímetro.

El frontend se publica en `https://ezevillo.com/truco-a-3/`. El origen de sus requests es
`https://ezevillo.com`, sin path. REST y WebSocket van directamente al servicio de Render;
el proxy del portfolio sólo sirve archivos del frontend.

## Documentación a actualizar

- `README.md`: perfil de producción, logs sin payloads y operación del despliegue nativo.
- `docs/CONTRATOS_API.md`: sin cambios de endpoints, DTOs, enums ni eventos; esta migración
  modifica el origen autorizado y la exposición de diagnóstico del perfil `prod`.

## Perfil de producción

Activar `SPRING_PROFILES_ACTIVE=prod` en Render **y durante el procesamiento AOT**.
El workflow `release.yml` compila y prueba el nativo con ese mismo perfil. Las condiciones
de Spring y los beans de Swagger se fijan al compilar; activar el perfil únicamente en
Render no recompone un binario nativo anterior.

Variables obligatorias del perfil:

| Variable | Uso |
| --- | --- |
| `SPRING_DATASOURCE_URL` | JDBC de PostgreSQL; conservar los parámetros TLS del proveedor |
| `TRUCO_DB_USER` | Usuario de la base |
| `TRUCO_DB_PASSWORD` | Contraseña de la base, administrada como secreto |
| `TRUCO_JWT_SECRET` | Secreto HS256 de al menos 32 bytes; no usar el default de desarrollo |
| `SPRING_PROFILES_ACTIVE` | `prod` |

`TRUCO_ALLOWED_ORIGINS` acepta una lista separada por comas de orígenes HTTPS explícitos
y el origen nativo exacto `capacitor://localhost`. Se conservan `https://localhost`
y `capacitor://localhost` para las aplicaciones Capacitor existentes. Durante la transición:
`https://ezevillo.com,https://www.ezevillo.com,https://ezevillo.github.io,https://localhost,capacitor://localhost`.
Agregar previews sólo por su origen exacto y cuando se necesiten. No autorizar
`*.netlify.app`, túneles ni localhost HTTP de desarrollo en producción. CORS y ambos endpoints WebSocket
usan la misma propiedad. CORS no reemplaza la autenticación ni bloquea clientes sin navegador.

El perfil deshabilita Swagger/API docs y la exposición de `/actuator/metrics`, y sólo
publica el estado de salud sin detalles. Se conservan métricas internas y request IDs.
El filtro HTTP registra método, path, estado y duración, sin cuerpos de requests/responses
ni headers de autenticación. Los request IDs externos tienen longitud y caracteres limitados.
Las credenciales de CI son sólo para la base efímera del runner; las reales se inyectan
en Render al ejecutar la imagen y nunca se guardan en el repositorio.

## Orden de publicación y recuperación

1. Verificar tests y `processAot` con perfil `prod`; generar la imagen mediante `release.yml`.
2. Conservar la imagen/digest anterior para rollback. No publicar cambios locales ajenos
   a la migración ni rotar el secreto JWT como parte automática de este cambio.
3. Configurar las variables anteriores sin reemplazar el resto de variables del servicio.
4. Desplegar el nuevo digest y usar `/actuator/health/readiness` como health check de Render:
   incluye base de datos y latido del scheduler; liveness sólo indica que el proceso vive.
5. Publicar el sitio de Truco y, al final, incorporar exclusivamente `/truco-a-3/*`
   al sitio Netlify que posee `ezevillo.com`.
6. Ante un fallo de frontend, retirar/revertir esa regla. Ante un fallo de backend,
   restaurar la imagen previa y su configuración compatible. Mantener el origen de
   GitHub Pages mientras se use como alternativa.

## Condiciones pendientes para disponibilidad de producción

- El servicio comprobado en Render usa plan **free** y una sola instancia. El plan gratuito
  puede suspenderse por inactividad; pasar a un plan permanente requiere decisión de costo.
- Verificar backups de PostgreSQL y ensayar una restauración antes de prometer recuperación.
- Configurar alertas externas sobre readiness y revisar fallos de despliegue y memoria.
- Incorporar límites de intentos en login/registro/invitados y límites de tráfico adecuados.
  Un gateway por sí solo no añade esas políticas.
- Auditar retención de logs anteriores y evaluar cambio de credenciales/sesiones expuestas
  a esos logs. El cambio del filtro sólo evita nuevos payloads después de publicar la imagen.
- Mantener **una instancia** hasta resolver la distribución del broker STOMP, presencia y
  schedulers. Subir réplicas sin esos cambios puede perder entregas y duplicar procesos.
- Un dominio como `api-truco.ezevillo.com` puede desacoplar la URL pública del proveedor;
  requiere alta en Render y DNS. No cambiar la URL del frontend antes de verificar TLS.

Estos puntos son requisitos operativos por verificar, no garantías que dé el build.

## Referencias

- [Ingreso HTTPS de Render](https://render.com/docs/web-services)
- [WebSockets](https://render.com/docs/websocket)
- [Health checks](https://render.com/docs/health-checks)
- [Límites del plan gratuito](https://render.com/docs/free)
- [Perfiles y AOT de Spring](https://docs.spring.io/spring-boot/how-to/aot.html)
