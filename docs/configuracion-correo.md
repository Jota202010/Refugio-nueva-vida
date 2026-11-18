# Configuración de notificaciones por correo

La aplicación notifica al usuario cuando una solicitud es preaprobada, queda pendiente de revisión humana o es rechazada. La preaprobación se guarda antes del envío: si el proveedor no está configurado o falla, la cita conserva su estado y el administrador puede reintentar el envío desde el detalle de la cita.

## Producción con Resend

En Railway se recomienda enviar mediante la API HTTPS de Resend. Crea una API key en Resend, verifica el dominio del remitente y configura estas variables en el servicio:

| Variable | Uso | Ejemplo |
|---|---|---|
| `MAIL_PROVIDER` | Proveedor de envío (`resend` o `smtp`) | `resend` |
| `RESEND_API_KEY` | Clave API secreta de Resend | Guardar solo como variable secreta |
| `MAIL_FROM` | Remitente autorizado en el dominio verificado | `Refugio Nueva Vida <notificaciones@tudominio.com>` |
| `APP_BASE_URL` | URL pública de la aplicación incluida en el correo | `https://refugio.example.com` |

No pegues la API key en el código ni en GitHub. `MAIL_FROM` debe pertenecer a un dominio que hayas verificado en Resend; una dirección Gmail personal no funciona como remitente verificado de ese proveedor. Tras agregar o cambiar variables en Railway, aplica los cambios y espera a que el servicio se despliegue.

El envío SMTP de Gmail en el servicio de producción falló al establecer la conexión TCP a `smtp.gmail.com` tanto en los puertos 587 como 465. Ese timeout ocurre antes de la autenticación, así que cambiar la contraseña de aplicación no resuelve esa falla de conectividad.

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
