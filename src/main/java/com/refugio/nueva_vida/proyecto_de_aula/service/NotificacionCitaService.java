package com.refugio.nueva_vida.proyecto_de_aula.service;

import com.refugio.nueva_vida.proyecto_de_aula.model.Cita;
import com.refugio.nueva_vida.proyecto_de_aula.repository.CitaRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class NotificacionCitaService {

    private final JavaMailSender mailSender;
    private final CitaRepository citaRepository;
    private final String remitente;
    private final String baseUrl;

    public NotificacionCitaService(JavaMailSender mailSender,
                                   CitaRepository citaRepository,
                                   @Value("${app.mail.from:no-reply@refugionuevavida.org}") String remitente,
                                   @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.mailSender = mailSender;
        this.citaRepository = citaRepository;
        this.remitente = remitente;
        this.baseUrl = baseUrl;
    }

    public void enviarAvisoPreaprobacion(Integer idCita) {
        Cita cita = citaRepository.findWithUsuarioAndPerroByIdCita(idCita)
            .orElseThrow(() -> new IllegalStateException("La cita ya no existe."));
        if (cita.getEstado() != Cita.EstadoCita.pre_aprobada) {
            throw new IllegalStateException("Solo se puede notificar una cita pre-aprobada.");
        }

        String enlace = UriComponentsBuilder.fromUriString(baseUrl)
            .path("/cita/{id}/elegir-horario")
            .buildAndExpand(idCita)
            .toUriString();
        String nombre = HtmlUtils.htmlEscape(cita.getUsuario().getNombre());
        String perro = HtmlUtils.htmlEscape(cita.getPerro().getNombre());

        enviarCorreo(cita, "¡Buenas noticias! Tu solicitud fue pre-aprobada",
            "Hola " + cita.getUsuario().getNombre() + ",\n\n"
                + "Tenemos buenas noticias: tu solicitud para conocer a "
                + cita.getPerro().getNombre()
                + " fue pre-aprobada. El siguiente paso es elegir el día y la hora de tu visita:\n"
                + enlace + "\n\n"
                + "Esta preaprobación te permite agendar una visita; la adopción aún no está confirmada.\n\n"
                + "Con cariño,\nRefugio Nueva Vida",
            "<div style=\"margin:0;padding:32px 12px;background:#f4f6f2;font-family:Arial,sans-serif;color:#24352e\">"
                + "<div style=\"max-width:560px;margin:0 auto;background:#fff;border:1px solid #e3e9e4;"
                + "border-radius:18px;overflow:hidden\">"
                + "<div style=\"padding:28px 32px;background:#315c48;color:#fff;text-align:center\">"
                + "<div style=\"font-size:14px;letter-spacing:1px\">REFUGIO NUEVA VIDA</div>"
                + "<div style=\"font-size:32px;margin-top:14px\">🐾</div>"
                + "<h1 style=\"font-size:24px;margin:8px 0 0\">¡Tenemos buenas noticias!</h1></div>"
                + "<div style=\"padding:28px 32px\">"
                + "<p style=\"font-size:16px\">Hola " + nombre + ",</p>"
                + "<p style=\"font-size:16px;line-height:1.7\">Tu solicitud para conocer a "
                + "<strong>" + perro + "</strong> fue pre-aprobada. ¡Nos alegra que quieras dar este paso!</p>"
                + "<div style=\"margin:24px 0;padding:18px;border-radius:12px;background:#f1f7f2;"
                + "border:1px solid #dceade\"><div style=\"font-size:13px;color:#557064\">TU PRÓXIMO PASO</div>"
                + "<div style=\"font-size:17px;font-weight:bold;margin-top:6px\">Elige el día y la hora de tu visita</div></div>"
                + "<p style=\"text-align:center;margin:28px 0\"><a href=\"" + HtmlUtils.htmlEscape(enlace)
                + "\" style=\"display:inline-block;background:#e8793e;color:#fff;padding:14px 24px;"
                + "border-radius:9px;text-decoration:none;font-weight:bold\">Elegir horario</a></p>"
                + "<p style=\"font-size:13px;color:#66756e;line-height:1.6\">Esta preaprobación te permite agendar "
                + "una visita; la adopción aún no está confirmada. Si el botón no funciona, copia este enlace:<br>"
                + "<a style=\"color:#315c48;word-break:break-all\" href=\"" + HtmlUtils.htmlEscape(enlace) + "\">"
                + HtmlUtils.htmlEscape(enlace) + "</a></p>"
                + "<p style=\"margin-top:26px\">Con cariño,<br><strong>Equipo Refugio Nueva Vida</strong></p>"
                + "</div></div></div>");
    }

    public void enviarAvisoRechazo(Integer idCita) {
        Cita cita = citaRepository.findWithUsuarioAndPerroByIdCita(idCita)
            .orElseThrow(() -> new IllegalStateException("La cita ya no existe."));
        if (cita.getEstado() != Cita.EstadoCita.rechazada) {
            throw new IllegalStateException("Solo se puede notificar una solicitud rechazada.");
        }

        String nombre = HtmlUtils.htmlEscape(cita.getUsuario().getNombre());
        String perro = HtmlUtils.htmlEscape(cita.getPerro().getNombre());
        String enlace = UriComponentsBuilder.fromUriString(baseUrl).path("/inicio").build().toUriString();
        enviarCorreo(cita, "Actualización sobre tu solicitud de adopción",
            "Hola " + cita.getUsuario().getNombre() + ",\n\n"
                + "Gracias por confiar en Refugio Nueva Vida y por tu interés en "
                + cita.getPerro().getNombre() + ". En esta ocasión no podremos avanzar con esta solicitud.\n\n"
                + "Sabemos que recibir esta noticia puede ser difícil. Esta decisión se refiere únicamente "
                + "a esta solicitud y no resta valor al cariño y compromiso que tienes para ofrecer. "
                + "Te agradecemos sinceramente el tiempo y la ilusión que compartiste con nosotros.\n\n"
                + "Con respeto,\nRefugio Nueva Vida",
            "<div style=\"margin:0;padding:32px 12px;background:#f4f6f2;font-family:Arial,sans-serif;color:#24352e\">"
                + "<div style=\"max-width:560px;margin:0 auto;background:#fff;border:1px solid #e3e9e4;"
                + "border-radius:18px;overflow:hidden\">"
                + "<div style=\"padding:26px 32px;background:#315c48;color:#fff;text-align:center\">"
                + "<div style=\"font-size:14px;letter-spacing:1px\">REFUGIO NUEVA VIDA</div>"
                + "<div style=\"font-size:30px;margin-top:12px\">🐾</div>"
                + "<h1 style=\"font-size:22px;margin:8px 0 0\">Una actualización sobre tu solicitud</h1></div>"
                + "<div style=\"padding:28px 32px\">"
                + "<p style=\"font-size:16px\">Hola " + nombre + ",</p>"
                + "<p style=\"font-size:16px;line-height:1.7\">Gracias por confiar en nosotros y por tu interés en "
                + "<strong>" + perro + "</strong>. Después de revisar la solicitud, en esta ocasión no podremos "
                + "avanzar con ella.</p>"
                + "<div style=\"margin:22px 0;padding:18px;border-left:4px solid #d6a56c;background:#faf7f1;"
                + "border-radius:8px\"><p style=\"margin:0;line-height:1.7\">Sabemos que recibir esta noticia puede "
                + "ser difícil. Esta decisión se refiere únicamente a esta solicitud y no resta valor al cariño "
                + "y compromiso que tienes para ofrecer.</p></div>"
                + "<p style=\"line-height:1.7\">Te agradecemos sinceramente el tiempo y la ilusión que compartiste "
                + "con nosotros. Si lo deseas, puedes conocer a los otros perros que esperan un hogar.</p>"
                + "<p style=\"text-align:center;margin:26px 0\"><a href=\"" + HtmlUtils.htmlEscape(enlace)
                + "\" style=\"display:inline-block;background:#315c48;color:#fff;padding:13px 22px;"
                + "border-radius:9px;text-decoration:none;font-weight:bold\">Conocer a los perros</a></p>"
                + "<p style=\"margin-top:26px\">Con respeto y gratitud,<br>"
                + "<strong>Equipo Refugio Nueva Vida</strong></p>"
                + "</div></div></div>");
    }

    private void enviarCorreo(Cita cita, String asunto, String textoPlano, String contenidoHtml) {
        MimeMessage mensaje = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(cita.getUsuario().getEmail());
            helper.setSubject(asunto);
            helper.setText(textoPlano, contenidoHtml);
        } catch (MessagingException e) {
            throw new MailPreparationException("No se pudo preparar el correo de notificación de cita.", e);
        }
        mailSender.send(mensaje);
    }
}
