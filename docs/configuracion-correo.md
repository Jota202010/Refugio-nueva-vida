# Configuración del correo de preaprobación

La aplicación envía al usuario un correo al preaprobar su solicitud. El correo incluye el perro solicitado y un enlace para iniciar sesión y elegir un horario. La preaprobación se guarda antes del envío: si el SMTP no está configurado o falla, la cita permanece preaprobada y el administrador puede reintentar el envío desde el detalle de la cita.

Configura estas variables de entorno en el equipo o servicio que ejecuta la aplicación:

| Variable | Uso | Ejemplo |
|---|---|---|
| `MAIL_HOST` | Servidor SMTP del proveedor de correo | `smtp.example.com` |
| `MAIL_PORT` | Puerto SMTP con STARTTLS | `587` |
| `MAIL_USERNAME` | Cuenta SMTP autenticada | Dirección de correo del remitente |
| `MAIL_PASSWORD` | Contraseña o clave de aplicación SMTP | No guardar en el repositorio |
| `MAIL_SMTP_AUTH` | Habilita autenticación SMTP | `true` |
| `MAIL_SMTP_STARTTLS` | Habilita STARTTLS | `true` |
| `MAIL_FROM` | Dirección que aparecerá como remitente | Dirección autorizada por el proveedor |
| `APP_BASE_URL` | URL pública de la aplicación incluida en el correo | `https://refugio.example.com` |

Usa las credenciales SMTP y el remitente autorizados por el proveedor. Para proveedores que requieren una clave de aplicación, usa esa clave en `MAIL_PASSWORD`, no la contraseña habitual de la cuenta. No uses `localhost` como `APP_BASE_URL` en una instalación accesible desde Internet: los usuarios recibirían un enlace que apunta a su propio equipo.

Si el envío falla, se registra el error técnico en el log del servidor y el administrador ve un aviso con la opción **Reenviar correo para elegir horario**. La falta de configuración SMTP no impide iniciar la aplicación ni revierte la preaprobación.
