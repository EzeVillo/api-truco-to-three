# Ingreso y perfil de producción

Verificar el código y la configuración del servicio antes de asumir que esta decisión
describe el estado desplegado. Preparar los archivos no publica una imagen.

- El monolito recibe REST y WebSocket mediante el ingreso HTTPS/WSS de Render. No se añade
  un gateway mientras no haya una política o un segundo servicio que lo requiera.
- La web se sirve desde `/truco-a-3/`, pero el origen autorizado es `https://ezevillo.com`.
  Se conservan GitHub Pages durante la migración y los orígenes exactos de Capacitor.
- `prod` exige credenciales externas, prohíbe el secreto conocido de desarrollo y valida
  orígenes sin comodines ni paths. Swagger y los detalles de diagnóstico no son públicos.
- Los logs HTTP no contienen cuerpos ni headers de autenticación. Se conserva el request ID,
  método, path, estado y duración.
- Spring AOT decide los beans de cada perfil al compilar. Compilar el nativo con `prod`
  y ejecutarlo con el mismo perfil; cambiar sólo la variable de Render no alcanza.

La guía operativa, las variables, la secuencia de publicación y los pendientes de disponibilidad
están en [Producción](../PRODUCCION.md).
