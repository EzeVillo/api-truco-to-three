# Ingreso y perfil de producción

Verificar el código y la configuración del servicio antes de asumir que esta decisión
describe el estado desplegado. Preparar los archivos no publica una imagen.

- Cloudflare Worker `truco-api` recibe REST y WebSocket, aplica límites por IP/ruta y
  reenvía a Render. Es independiente del Worker de IBIT y no modifica el DNS del portfolio.
- Render exige `X-Truco-Gateway-Secret` cuando `TRUCO_GATEWAY_ENABLED=true`, para impedir
  el bypass por `onrender.com`. Sólo GET/HEAD de los probes exactos quedan exceptuados.
  La clave se guarda en Cloudflare/Render, nunca en el cliente ni los logs.
- Los contadores son aproximados y por ubicación de Cloudflare. No prometen cuotas
  globales exactas ni limitan frames WebSocket; el chat conserva su cooldown propio.
- La web se sirve desde `/truco-a-3/`, pero el origen autorizado es `https://ezevillo.com`.
  GitHub Pages se retira y no se mantiene como publicación alternativa. Se conservan
  los orígenes exactos de Capacitor para las aplicaciones nativas.
- `prod` exige credenciales externas, prohíbe el secreto conocido de desarrollo y valida
  orígenes sin comodines ni paths. Swagger y los detalles de diagnóstico no son públicos.
- Los logs HTTP no contienen cuerpos ni headers de autenticación. Se conserva el request ID,
  método, path, estado y duración.
- Spring AOT decide los beans de cada perfil al compilar. Compilar el nativo con `prod`
  y ejecutarlo con el mismo perfil; cambiar sólo la variable de Render no alcanza.

La guía operativa, las variables, la secuencia de publicación y los pendientes de disponibilidad
están en [Producción](../PRODUCCION.md).
