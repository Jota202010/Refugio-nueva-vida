# Configuración de notificaciones por correo

La aplicación notifica al usuario cuando una solicitud es preaprobada, queda pendiente de revisión humana o es rechazada. La preaprobación se guarda antes del envío: si el proveedor no está configurado o falla, la cita conserva su estado y el administrador puede reintentar el envío desde el detalle de la cita.

## Producción con Gmail API

Gmail API envía mediante HTTPS y evita la conexión SMTP saliente que no está disponible en Railway. Usa el mismo proyecto de Google Cloud del cliente OAuth de la aplicación:

| Variable | Uso | Ejemplo |
|---|---|---|
| `MAIL_PROVIDER` | Proveedor de envío (`gmail-api`, `resend` o `smtp`) | `gmail-api` |
| `GOOGLE_CLIENT_ID` | ID del cliente OAuth web existente | Guardar como variable secreta |
| `GOOGLE_CLIENT_SECRET` | Secreto del mismo cliente OAuth | Guardar solo como variable secreta |
| `GMAIL_TOKEN_ENCRYPTION_KEY` | Secreto independiente para cifrar el refresh token guardado | Generar uno aleatorio y persistente |
| `MAIL_FROM` | Opcional: Gmail conectado o alias autorizado; si se omite, usa la cuenta conectada | `avisos@gmail.com` |
| `APP_BASE_URL` | URL pública de la aplicación incluida en el correo | `https://refugio.example.com` |

En Google Cloud Console, habilita **Gmail API** y agrega `https://www.googleapis.com/auth/gmail.send` a los scopes del consentimiento OAuth. La aplicación registra este permiso en una conexión separada: no se añade al acceso de Google usado para iniciar sesión. Agrega la cuenta del refugio como usuario de prueba si la aplicación OAuth está en modo de pruebas.

En el cliente OAuth web agrega como URI de redirección autorizado `<APP_BASE_URL>/login/oauth2/code/google-mail`. Conserva también el URI existente `<APP_BASE_URL>/login/oauth2/code/google` para el inicio de sesión habitual.

Después de configurar las variables y desplegar, inicia sesión como administrador, abre **Configuración de correo** desde el panel y selecciona **Conectar con Google**. Acepta el permiso de envío. El refresh token se guarda cifrado en la base de datos. Conserva el mismo `GMAIL_TOKEN_ENCRYPTION_KEY` en los despliegues posteriores; si se pierde o cambia, habrá que volver a conectar Gmail. Guárdalo como secreto y no lo copies al código ni lo publiques en GitHub. Puedes generar un valor con `openssl rand -base64 32`.

Google puede expirar los refresh tokens de aplicaciones externas que permanecen en modo de pruebas después de siete días. Para uso continuo, revisa el estado de publicación y verificación de la pantalla de consentimiento OAuth. Si se revoca o vence el permiso, vuelve a conectar la cuenta desde administración.

Si configuras `MAIL_FROM`, debe ser el Gmail conectado o un alias de envío autorizado en esa cuenta. Si no se configura, la aplicación usa automáticamente el Gmail conectado. Nunca uses una dirección que no pertenezca a la cuenta conectada.

## Alternativa con Resend

Resend sigue disponible si se cuenta con un dominio propio verificado. Configura `MAIL_PROVIDER=resend`, `RESEND_API_KEY` como variable secreta y `MAIL_FROM` con una dirección de ese dominio. Una dirección Gmail personal no funciona como remitente verificado de Resend.

El envío SMTP de Gmail en el servicio de producción falló al establecer la conexión TCP a `smtp.gmail.com` tanto en los puertos 587 como 465. Gmail API es la alternativa HTTPS para ese entorno.

## SMTP local

Para Mailpit en Docker, deja `MAIL_PROVIDER=smtp` (valor predeterminado) y configura:

| Variable | Uso | Ejemplo |
|---|---|---|
| `MAIL_HOST` | Servidor SMTP | `mailpit` |
| `MAIL_PORT` | Puerto SMTP | `1025` |
| `MAIL_USERNAME` | Cuenta SMTP si el servidor la requiere | Vacío para Mailpit |
| `MAIL_PASSWORD` | Contraseña SMTP si el servidor la requiere | Vacío para Mailpit |
| `MAIL_SMTP_AUTH` | Habilita autenticación SMTP | `false` |
| `MAIL_SMTP_STARTTLS` | Habilita STARTTLS | `false` |

Para otros servidores SMTP, usa las credenciales y el remitente autorizados por el proveedor. No uses `localhost` como `APP_BASE_URL` en una instalación accesible desde Internet: los usuarios recibirían un enlace que apunta a su propio equipo.

Si el envío falla, el administrador ve un aviso y puede reintentar desde el detalle de la cita. La falta de configuración del proveedor no impide iniciar la aplicación ni revierte el estado de la solicitud.
