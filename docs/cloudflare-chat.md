# Proxy de Gemini con Cloudflare Workers

El Worker en `cloudflare-chat/` adapta las solicitudes del chat al API de Gemini y usa `gemini-3.5-flash-lite`. El backend añade a cada consulta una lista acotada de perros publicados con los datos visibles en sus perfiles y una descripción breve del flujo de adopción. Nunca envía solicitudes, usuarios, datos médicos privados ni información de cuenta. La clave se guarda como secreto en Cloudflare: no va en Git, en Docker ni en el navegador. La configuración limita el proxy a 12 consultas por minuto por ubicación de Cloudflare; los contadores del Rate Limiting API son por ubicación, no un contador global exacto. La aplicación también limita las solicitudes por sesión.

## Publicar el Worker una vez

Se necesita una cuenta gratuita de Cloudflare, Node.js instalado y una clave Gemini creada en [Google AI Studio](https://aistudio.google.com/app/apikey). La disponibilidad y cuota del modelo gratuito dependen de las condiciones actuales de Google. En el nivel gratuito, Google indica que el contenido puede usarse para mejorar sus productos; no envíes información sensible.

Desde PowerShell, en la raíz del repositorio:

```powershell
cd cloudflare-chat
npx wrangler@latest login
npx wrangler@latest deploy
```

El Worker puede quedar publicado inicialmente sin clave y devolverá un estado `configured:false`. Para añadirla desde el dashboard, abre **Workers & Pages → refugio-gemini-proxy → Settings → Variables and secrets → Add variable**. Selecciona **Production**, escribe `GEMINI_API_KEY` como nombre, pega la clave como valor, activa **Secret** y pulsa **Add variable and deploy**. No la pegues en comandos, archivos, Git ni conversaciones. También se puede usar `npx wrangler secret put GEMINI_API_KEY`, que solicita el valor de forma interactiva.

El `namespace_id` de `wrangler.jsonc` debe ser único dentro de tu cuenta de Cloudflare; si ya lo usas, cámbialo por otro número entero que no esté en uso.

Guarda la URL HTTPS que Wrangler muestra al terminar. Comprueba `https://TU-WORKER.workers.dev/healthz`; debe responder `{"status":"ok","configured":true}`. El endpoint de salud no revela la clave.

## Conectar Docker

La URL HTTPS del Worker está configurada como valor predeterminado de `GEMINI_PROXY_URL` en `compose.yaml`. No es secreta ni requiere un `.env`; por tanto, una vez publicado el proxy y guardada la clave en Cloudflare, el profesor solo tiene que ejecutar:

```bash
docker compose up
```

Reconstruye y prueba la app antes de compartir el repositorio. El endpoint público del Worker puede ser invocado por cualquiera; el límite de 12 consultas/minuto reduce el uso accidental, pero no es una cuota global ni una garantía contra abuso. Cloudflare Workers Free incluye límites diarios sujetos a sus términos vigentes y Gemini tiene cuotas separadas. Al agotarse una cuota, el chat puede dejar de responder hasta que se restablezca. No se garantiza disponibilidad permanente ni costo cero si se habilitan productos de pago.

## Pruebas locales del Worker

Con Node.js instalado, desde `cloudflare-chat/`:

```powershell
node --test test/worker.test.mjs
```
