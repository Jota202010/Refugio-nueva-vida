# Refugio Nueva Vida

Aplicación web de adopción de perros con Spring Boot, MySQL y notificaciones por correo.

## IMPORTANTE — Profesor Ronald: cambios implementados durante el Semestre 5



### Gestión de horarios y citas

- Se incorporó un panel administrativo para configurar los horarios disponibles por cada día de la semana.
- Se pueden registrar y eliminar excepciones para fechas específicas, por ejemplo, días en los que el refugio no atenderá.
- El sistema genera los turnos disponibles de acuerdo con la configuración y la duración establecida.
- Cuando una solicitud de cita es preaprobada, el usuario puede seleccionar un horario disponible y confirmar la cita.
- Se reforzaron las validaciones de fechas, horas, estado de la cita, disponibilidad del turno y solicitudes duplicadas.
- Se mejoró el seguimiento administrativo de solicitudes: preaprobar, rechazar y confirmar adopciones, con sus correspondientes estados y mensajes.

### Notificaciones por correo

- Al preaprobar una solicitud, se envía un correo con información para que el usuario pueda elegir un horario.
- Se agregó la notificación de rechazo y la opción administrativa para reenviar notificaciones.
- Un problema con el correo no revierte una preaprobación ya guardada; el administrador puede intentar enviar la notificación nuevamente.
- En Docker, los mensajes se capturan en Mailpit para probarlos sin enviarlos a direcciones externas.

### Chat de soporte con Gemini

- Se integró el chat de ayuda con el backend y un proxy desplegado en Cloudflare Workers.
- El asistente puede responder preguntas utilizando información pública de los perros publicados.
- No se envían al modelo datos de cuentas, solicitudes de adopción ni registros médicos privados, y las conversaciones no se guardan en la base de datos.
- La clave de Gemini se administra como secreto en Cloudflare; no se incluye en Git, Docker ni el navegador.
- Se configuró un límite de solicitudes en el proxy. Sus contadores son por ubicación de Cloudflare, no un límite global; también aplican las cuotas de Cloudflare y Gemini.

### Experiencia visual, navegación y accesibilidad

- Se renovó el tema visual con verde bosque, blanco, tonos salvia y colores oscuros en las páginas públicas y administrativas.
- Se ajustaron botones, tarjetas, formularios, tablas, estados de foco y presentación para pantallas pequeñas.
- En el inicio, la búsqueda funciona al escribir y al pulsar el botón; los filtros de perros por sexo son botones accesibles y actualizan el contador.
- Se añadió un mensaje cuando no hay perros que coincidan con la búsqueda o los filtros.
- Se corrigió el enlace de regreso de la página de error para que funcione aunque el usuario haya abierto esa página directamente.

### Correcciones, ejecución y pruebas

- Se eliminó una captura silenciosa de errores en la consulta de citas desde el detalle de una mascota; un fallo ya no se presenta como si el usuario no tuviera una solicitud activa.
- Se preparó Docker Compose para iniciar la aplicación y MySQL con `docker compose up`, sin instalar Java, Maven o MySQL en el equipo del profesor.
- Se agregaron datos de demostración: una cuenta administrativa, tres cuentas de usuario y seis perros con fotos ilustrativas incluidas en el proyecto. Las fotos cargadas desde la aplicación se guardan en un volumen local y no se publican en Git.
- Se verificaron las pruebas automatizadas del backend y del Worker, la configuración de Docker y las rutas públicas principales.

Para iniciar la demostración y consultar las credenciales de prueba, continúa con las instrucciones de Docker y [los datos de demostración](docs/demo-data.md).

## Ejecutar el proyecto con Docker

Requisitos: Docker Desktop instalado y en ejecución. No hace falta instalar Java, Maven ni MySQL en el equipo.

Desde la raíz del repositorio, ejecuta:

```bash
docker compose up
```

La primera vez, Docker construye la aplicación y descarga las imágenes necesarias. Cuando los contenedores estén listos, abre:

- Aplicación: <http://localhost:8080>
- Buzón local para revisar correos: <http://localhost:8025>

La demostración crea una cuenta administrativa, tres cuentas de usuario y seis perros con fotos de ejemplo. Consulta [docs/demo-data.md](docs/demo-data.md) para las credenciales y los créditos de las imágenes. Puedes registrarte desde la aplicación para probar el flujo como usuario. El correo de preaprobación se captura en Mailpit y no se envía a direcciones externas.

El esquema MySQL se inicializa desde `docs/script SQL.sql`. La base de datos y las fotos subidas se conservan en volúmenes de Docker al detener los contenedores con `Ctrl+C`. Los correos de preaprobación y rechazo se capturan en Mailpit para revisar su presentación sin enviarlos a direcciones reales. Para detenerlos desde otra terminal usa:

```bash
docker compose down
```

Estos valores son solo para la demostración local, y los puertos se publican únicamente en `localhost`. No publiques este Compose en Internet con las credenciales de demostración. Para reemplazar la contraseña del administrador antes de la primera ejecución, establece `DEMO_ADMIN_PASSWORD` en el entorno. Las variables `DB_PASSWORD` y `MYSQL_ROOT_PASSWORD` también permiten cambiar las contraseñas locales.

La cuenta de demostración se crea solo si no existe ya un usuario `admin`. Si cambias `DEMO_ADMIN_PASSWORD` después de que el volumen de base de datos ya tenga datos, no se modifica una cuenta existente.

## Correo en Docker

El envío SMTP de la aplicación apunta al buzón local Mailpit. Los mensajes capturados pueden consultarse en <http://localhost:8025>. No se necesita una cuenta Gmail ni SMTP externo para la demostración.

## Chat de soporte con Gemini

El botón de soporte aparece en las páginas de usuario. Para que el profesor no tenga que configurar una clave, el proyecto incluye un proxy para Cloudflare Workers en `cloudflare-chat/`. La clave de Gemini se guarda como secreto en Cloudflare; no se incluye en Docker, en el navegador ni en Git.

El Worker está publicado en Cloudflare y su URL HTTPS está configurada como valor predeterminado de `GEMINI_PROXY_URL` en `compose.yaml`. El backend consulta los perros publicados y comparte con Gemini únicamente información pública de sus perfiles; no envía usuarios, solicitudes ni registros médicos privados. La clave Gemini se conserva como secreto en Cloudflare, fuera de Git y Docker. La configuración limita solicitudes a 12 por minuto por ubicación de Cloudflare; no es un contador global exacto. Aplican además los límites diarios de Cloudflare y las cuotas de Gemini, por lo que el chat podría dejar de responder al agotarlas. Las conversaciones no se guardan en la base de datos. Consulta [la guía de Cloudflare Workers](docs/cloudflare-chat.md) para volver a publicar o administrar el proxy.

Como alternativa de desarrollo local, puedes usar una clave directa en `GEMINI_API_KEY` dentro del `.env` local. No compartas ese archivo ni lo subas a GitHub. El nivel gratuito de Gemini tiene cuotas y términos propios; consulta [los precios y condiciones actuales](https://ai.google.dev/gemini-api/docs/pricing).

## Desarrollo fuera de Docker

La aplicación usa `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` desde el entorno; por defecto intenta conectarse a MySQL local como `root` sin contraseña. Para el correo, configura `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`, `MAIL_FROM` y `APP_BASE_URL`. Para activar Gemini fuera de Docker, configura `GEMINI_PROXY_URL` para usar el proxy o `GEMINI_API_KEY` para desarrollo local directo. No guardes contraseñas ni claves API en el repositorio.
