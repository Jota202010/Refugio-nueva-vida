# Historias de usuarios — Refugio Nueva Vida

Este documento describe las funciones disponibles en la aplicación según el rol. Las historias reflejan el comportamiento implementado en el proyecto; no implican que el chat pueda realizar acciones en nombre de una persona ni que la solicitud de adopción se apruebe automáticamente.

## Visitante

- Como visitante, quiero ver los perros publicados y disponibles para adopción, para conocer cuáles puedo considerar.
- Como visitante, quiero buscar y filtrar los perros del catálogo, para encontrar perfiles que coincidan con mis intereses.
- Como visitante, quiero abrir el perfil de un perro y consultar sus fotos e información pública, para conocerlo antes de registrarme.
- Como visitante, quiero conocer la información institucional del refugio, su misión y propósito, para saber más sobre la organización.
- Como visitante, quiero crear una cuenta con mis datos de contacto y contraseña, para iniciar el proceso de adopción.
- Como visitante, quiero iniciar sesión con mi cuenta y cerrar la sesión de forma segura, para acceder a las funciones personales.

## Usuario registrado

- Como usuario, quiero consultar mis datos de perfil y las solicitudes asociadas a mi cuenta, para revisar mi información y actividad.
- Como usuario, quiero enviar una solicitud de adopción para un perro publicado, incluyendo la información solicitada sobre mi vivienda, para que el refugio evalúe mi caso.
- Como usuario, quiero evitar enviar solicitudes activas duplicadas para el mismo perro, para no repetir el trámite por error.
- Como usuario, quiero revisar desde mi perfil mis solicitudes y citas junto con su estado, para conocer el avance de cada proceso.
- Como usuario, quiero recibir una notificación por correo cuando el refugio preapruebe o rechace mi solicitud, para enterarme de la decisión.
- Como usuario con una solicitud preaprobada, quiero consultar los horarios disponibles y seleccionar uno, para reservar una visita al refugio.
- Como usuario, quiero que el sistema valide la disponibilidad del horario al confirmar la visita, para evitar reservar un turno ocupado o no habilitado.
- Como usuario, quiero conversar con el asistente de soporte sobre el sitio, las adopciones y los perros publicados, para resolver dudas generales.
- Como usuario, quiero que el asistente use solo datos públicos del catálogo y no acceda a mi cuenta, mis solicitudes ni a información médica privada, para proteger mis datos personales.

## Administrador

- Como administrador, quiero acceder a un panel que reúne los perros, usuarios, solicitudes y cantidad de solicitudes pendientes, para supervisar la operación del refugio.
- Como administrador, quiero consultar la ficha y el historial de un perro, incluyendo sus solicitudes asociadas, para revisar su situación.
- Como administrador, quiero registrar perros con sus datos de identificación, sexo, origen, salud, sociabilidad, vacunación, esterilización, descripción y estado de publicación, para mantener actualizado el catálogo y el registro interno.
- Como administrador, quiero editar la información del perro y actualizar su estado de publicación, para reflejar cambios en su disponibilidad y situación.
- Como administrador, quiero registrar cambios de estado en el historial del perro, para conservar la trazabilidad de sus cambios de situación.
- Como administrador, quiero subir varias fotos a la ficha de un perro y elegir una foto de perfil, para presentar su perfil visualmente.
- Como administrador, quiero eliminar fotos de una ficha, para retirar imágenes que ya no deban mostrarse.
- Como administrador, quiero eliminar un perro cuando corresponda, y recibir un aviso si existen citas asociadas que impiden hacerlo, para evitar borrar registros relacionados por accidente.
- Como administrador, quiero consultar la información de un usuario y sus solicitudes, para revisar su participación en los procesos de adopción.
- Como administrador, quiero consultar el detalle de cada solicitud y su estado, para evaluar el caso antes de tomar una decisión.
- Como administrador, quiero preaprobar una solicitud, para habilitar que el usuario seleccione un horario de visita y recibir la información correspondiente por correo.
- Como administrador, quiero rechazar una solicitud y notificar al usuario, para comunicarle la decisión del refugio.
- Como administrador, quiero reenviar el correo de preaprobación o rechazo si el envío falla, para que el usuario reciba la notificación sin perder el estado ya guardado de la solicitud.
- Como administrador, quiero confirmar la adopción después de completar el proceso, para actualizar el estado del perro y vincular el resultado con el usuario y la solicitud.
- Como administrador, quiero configurar los días y rangos de atención semanales, la duración de cada turno y la capacidad, para controlar los horarios que se ofrecen a los usuarios.
- Como administrador, quiero registrar excepciones para fechas concretas y eliminarlas cuando ya no apliquen, para ajustar la disponibilidad ante cierres o cambios puntuales.
- Como administrador, quiero consultar métricas de perros, solicitudes y usuarios, incluyendo estados y tiempos de respuesta, para hacer seguimiento a la actividad del refugio.
- Como administrador, quiero consultar mi perfil administrativo y el resumen de mis solicitudes, para revisar mi actividad.

## Funciones de API

- Como consumidor autorizado de la API, quiero enviar credenciales a `POST /api/auth/token` y recibir un JWT válido, para autenticar solicitudes a los endpoints de API protegidos.
- Como consumidor autorizado de la API, quiero enviar mi JWT a `GET /api/auth/me` y obtener mi identidad y roles, para confirmar el contexto de autenticación.
- Como consumidor de la aplicación web, quiero conservar el inicio de sesión tradicional por formulario y sesión, para usar la web sin tener que reemplazar ese flujo por JWT.

## Alcance del asistente

El chat responde preguntas y no puede crear solicitudes, aprobar adopciones, confirmar citas ni modificar perfiles. Para responder sobre los perros, el backend consulta los perfiles publicados y envía a Gemini un contexto limitado con información pública; el modelo no se conecta directamente a la base de datos.
