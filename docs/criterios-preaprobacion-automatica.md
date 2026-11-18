Criterios de preaprobacion automatica de solicitudes

1. Alcance

La IA realiza una evaluacion inicial de la solicitud para decidir si puede quedar **preaprobada para que el solicitante agende una visita** o si requiere revision humana. No aprueba ni rechaza una adopcion definitiva. El administrador conserva la facultad de revisar, preaprobar o rechazar manualmente los casos pendientes, y la adopcion solo se registra despues de la visita y del proceso del refugio.

La regla es deliberadamente conservadora: **la IA nunca rechaza automaticamente**. Una respuesta negativa, ausente, contradictoria, una incompatibilidad posible o cualquier incertidumbre deja la solicitud en espera para que la revise un trabajador.

2. Condiciones para consultar a la IA

El sistema solo pide una decision al modelo si se cumplen todas estas condiciones: (Se tienen que cumplir todos los escenarios en conjunto)

2.1: El solicitante acepta expresamente el analisis automatizado opcional.
2.2: Estan presentes las respuestas estructuradas necesarias: tipo y propiedad de vivienda, permiso para mascotas cuando aplica, acuerdo del hogar, mascotas actuales, tiempo que el perro estaria solo, posibilidad de pasearlo, capacidad de cubrir gastos veterinarios y emergencias, y las tres aceptaciones del refugio.
2.3: Todas las personas del hogar estan de acuerdo.
2.4: Si la vivienda es alquilada, el solicitante indica que se permiten mascotas.
2.5: El solicitante indica que puede pasear al perro y cubrir los gastos veterinarios y emergencias.
2.6: Acepta no abandonar al animal, el seguimiento del refugio y el proceso de evaluacion.
2.7: El solicitante no tiene otras mascotas actualmente. Si las tiene, la compatibilidad necesita revision humana porque el formulario no recoge suficiente informacion sobre esos animales.
2.8: El perro tiene informacion publica suficiente para valorar compatibilidad general. (Criterio a razonamiento de Gemini)
2.9: El perro estaria solo menos de dos horas o entre dos y cuatro horas al dia. Las opciones de cuatro a seis horas y mas de seis horas pasan a revision humana: el refugio aun no ha definido un umbral de cuidado para esos casos.

La experiencia previa con perros no es requisito y no se usa para favorecer o perjudicar al solicitante. Tampoco se infieren ingresos, confiabilidad, personalidad u otras caracteristicas a partir de su forma de escribir.

3. Regla de Gemini

Cuando pasan las condiciones anteriores, Gemini recibe respuestas estructuradas del formulario y un extracto limitado del perfil **publico** del perro (edad, sexo, sociabilidad y descripcion publica). No recibe nombre, usuario, correo, telefono, direccion, persona responsable ni los textos libres de motivacion o historial de mascotas.

Gemini solo puede devolver una de estas decisiones:

- `PRE_APPROVE`: no hay contradiccion o incompatibilidad evidente en los datos permitidos.
- `HUMAN_REVIEW`: detecta un posible conflicto, faltan datos utiles o no puede concluir con claridad.

Una respuesta no valida, vacia, fuera del formato esperado, un error de Gemini, una cuota agotada o un timeout **nunca** se interpreta como aprobacion: la solicitud permanece en espera para revision humana. El backend valida de nuevo la decision y las condiciones antes de cambiar el estado.

4. Datos, transparencia y almacenamiento

- El consentimiento para el analisis es opcional. Si no se acepta, la solicitud se guarda y pasa directamente a revision humana.
- Antes de enviar el formulario, el usuario ve que las respuestas estructuradas seleccionadas y datos publicos del perro se procesaran con Google Gemini a traves del proxy de Cloudflare. No se envian identificadores de cuenta ni los campos de texto libre.
- El resultado, el motivo breve, la fecha de evaluacion y el consentimiento quedan asociados a la solicitud para que el administrador pueda auditar el flujo.
- Al recibir una preaprobacion automatica, el usuario recibe el correo existente para elegir un horario. La notificacion fallida no revierte la decision y se puede reenviar desde administracion.
- Los casos de revision humana no se rechazan ni reciben correo de preaprobacion automaticamente.

5. Limites operativos

El rango de cuatro a seis horas y el de mas de seis horas quedan en revision humana como medida conservadora, no como una conclusion veterinaria. El refugio debe revisar esta politica con su equipo y actualizarla si define criterios formales diferentes. Gemini puede equivocarse; la preaprobacion solo habilita la visita y no garantiza una adopcion.

La evaluacion usa la misma API key, modelo, cuotas y proxy de Cloudflare que el chat, pero en un endpoint separado con instrucciones y validacion propias. No se debe colocar la clave en el navegador ni en el repositorio. La disponibilidad del servicio externo no es garantizada.

Para habilitar el endpoint de evaluacion en el proxy publicado, hay que desplegar la version actualizada de `cloudflare-chat/` con Wrangler. Hasta que esa version este desplegada, un error del endpoint deja la solicitud en espera para revision humana. Con la configuracion actual, Hibernate agrega las columnas nuevas al arrancar (`ddl-auto=update`); `actualizacion-evaluacion-ia.sql` es solo una alternativa para instalaciones que administran el esquema manualmente.
