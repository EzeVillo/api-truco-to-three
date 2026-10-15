# Producción y migración a ezevillo.com

## Ingreso y rate limiting externo

El Worker `truco-api` recibe REST y WebSocket/STOMP por HTTPS/WSS, aplica rate limiting
y reenvía al único backend en Render. Su URL pública es
`https://truco-api.ezevillodev.workers.dev`. No se modifica el DNS de DonWeb ni la raíz.
Es independiente del Worker de IBIT. El código y su configuración están en `gateway/`.

El frontend se publica en `https://ezevillo.com/truco-a-3/`. El origen de sus requests es
`https://ezevillo.com`, sin path. REST y WebSocket pasan por el Worker;
el proxy del portfolio sólo sirve archivos del frontend.

### Políticas por minuto e IP

| Política | Solicitudes |
| --- | --- |
| General | 300 |
| Login | 10 |
| Registro | 5 |
| Invitado | 10 |
| Refresh | 60 |
| Apertura WebSocket / info de SockJS | 20 |

Cloudflare aporta `CF-Connecting-IP`; no se confía en `X-Forwarded-For` del cliente.
Los preflights no consumen cuota. Un exceso devuelve `429 RateLimitExceededException`
y `Retry-After: 60`; CORS permite leer ese header. No se cachean respuestas de API ni
se registran cuerpos, credenciales o IPs en logs propios del Worker. El proxy conserva
el upgrade WebSocket sin aceptar ni inspeccionar frames. Los upgrades usan `fetch` sin opciones
de caché de Cloudflare: aplicarles `cf.cacheTtl` bloqueaba los frames después del `101`.
La política HTTP de no caché se aplica sólo al resto de las solicitudes. El chat mantiene su límite
de dominio. Los contadores son aproximados y locales a cada ubicación de Cloudflare.

### Protección del origen y operación del Worker

`TRUCO_GATEWAY_ENABLED=true` en Render exige un secreto de al menos 32 bytes UTF-8 en
`TRUCO_GATEWAY_SECRET`. El Worker conserva el mismo valor en su secreto `GATEWAY_SECRET`
y sobrescribe el header privado antes de contactar a Render. Nunca poner esa clave en
el frontend, TOML, logs ni GitHub. JWT sigue identificando al usuario independientemente.
Los secretos productivos los genera y carga el usuario: el agente trabaja con nombres
y configuración, sin consultar sus valores. Los tests usan fixtures aisladas de producción.
Sólo GET/HEAD de `/actuator/health/liveness` y `/actuator/health/readiness` quedan exceptuados.
Una llamada directa a REST, wake o WebSocket devuelve `403 GatewayAccessDeniedException`.
En desarrollo el filtro está desactivado por defecto; el bean siempre se compila en AOT.

```powershell
cd gateway
pnpm install --frozen-lockfile
pnpm test
pnpm run deploy:check
pnpm run deploy
# Ingresar la clave mediante stdin; nunca como argumento visible.
pnpm exec wrangler secret put GATEWAY_SECRET
```

La CI verifica el Worker con orígenes efímeros locales, sin secretos productivos.
`native-realtime.yml` comprueba la imagen publicada contra PostgreSQL aislado: autentica
un invitado de prueba, completa STOMP y recibe un evento privado, con y sin el Worker.
Sus despliegues usan Wrangler con la cuenta Cloudflare del operador;
no copiar su token OAuth amplio a GitHub Actions. Publicar primero
el Worker y su secreto, después el frontend y por último Render con la protección activa.
Para rollback al frontend directo anterior, desactivar primero `TRUCO_GATEWAY_ENABLED`;
eso abre temporalmente el bypass y debe revertirse al terminar la incidencia. No rotar JWT
ni credenciales de PostgreSQL.

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
y `capacitor://localhost` para las aplicaciones Capacitor existentes. La configuración final es:
`https://ezevillo.com,https://www.ezevillo.com,https://localhost,capacitor://localhost`.
GitHub Pages se retira como web de producción y su origen no se mantiene autorizado.
Actualizar también `TRUCO_ALLOWED_ORIGINS` **en Render**, porque el valor externo tiene
precedencia sobre el YAML. La actualización por MCP conserva las otras variables y dispara
un redespliegue para aplicarlas. Verificar el deploy devuelto y no disparar otro duplicado.
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
   restaurar la imagen previa y su configuración compatible. Retirar GitHub Pages una vez
   publicado el frontend nuevo y quitar su origen de la configuración efectiva de Render.
   El rollback del frontend usa los despliegues de Netlify, sin reactivar la web anterior.

## Condiciones pendientes para disponibilidad de producción

- El servicio comprobado en Render usa plan **free** y una sola instancia. El plan gratuito
  puede suspenderse por inactividad; pasar a un plan permanente requiere decisión de costo.
- Verificar backups de PostgreSQL y ensayar una restauración antes de prometer recuperación.
- Configurar alertas externas sobre readiness y revisar fallos de despliegue y memoria.
- Revisar los umbrales del Worker con tráfico real y alertas de `429`/`503`; las cuotas
  por IP se comparten entre usuarios detrás del mismo NAT.
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
