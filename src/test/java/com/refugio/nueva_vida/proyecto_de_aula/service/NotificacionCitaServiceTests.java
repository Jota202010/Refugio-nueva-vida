package com.refugio.nueva_vida.proyecto_de_aula.service;

import com.refugio.nueva_vida.proyecto_de_aula.model.Cita;
import com.refugio.nueva_vida.proyecto_de_aula.model.Perro;
import com.refugio.nueva_vida.proyecto_de_aula.model.Usuario;
import com.refugio.nueva_vida.proyecto_de_aula.repository.CitaRepository;
import jakarta.mail.Session;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificacionCitaServiceTests {

    private JavaMailSender mailSender;
    private CitaRepository citaRepository;
    private NotificacionCitaService notificacionService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        citaRepository = mock(CitaRepository.class);
        notificacionService = new NotificacionCitaService(
            mailSender, citaRepository, "notificaciones@refugio.test", "https://refugio.test");
    }

    @Test
    void enviaCorreoConEnlaceParaElegirHorarioALaPersonaDeLaCita() throws Exception {
        Cita cita = crearCitaPreaprobada();
        MimeMessage mensaje = new MimeMessage(Session.getInstance(new Properties()));
        when(citaRepository.findWithUsuarioAndPerroByIdCita(45)).thenReturn(Optional.of(cita));
        when(mailSender.createMimeMessage()).thenReturn(mensaje);

        notificacionService.enviarAvisoPreaprobacion(45);
        mensaje.saveChanges();

        assertEquals("usuario@example.com", mensaje.getRecipients(MimeMessage.RecipientType.TO)[0].toString());
        assertEquals("¡Buenas noticias! Tu solicitud fue pre-aprobada", mensaje.getSubject());
        assertTrue(mensaje.getContentType().contains("multipart/"));
        String contenido = contenidoComoTexto(mensaje.getContent());
        assertTrue(contenido.contains("https://refugio.test/cita/45/elegir-horario"));
        assertTrue(contenido.contains("Esta preaprobación te permite agendar una visita"));
        assertTrue(contenido.contains("¡Tenemos buenas noticias!"));
        verify(mailSender).send(mensaje);
    }

    @Test
    void enviaAvisoDeRechazoConMensajeEmpaticoYEnlaceAlRefugio() throws Exception {
        Cita cita = crearCitaPreaprobada();
        cita.setEstado(Cita.EstadoCita.rechazada);
        MimeMessage mensaje = new MimeMessage(Session.getInstance(new Properties()));
        when(citaRepository.findWithUsuarioAndPerroByIdCita(45)).thenReturn(Optional.of(cita));
        when(mailSender.createMimeMessage()).thenReturn(mensaje);

        notificacionService.enviarAvisoRechazo(45);
        mensaje.saveChanges();

        assertEquals("usuario@example.com", mensaje.getRecipients(MimeMessage.RecipientType.TO)[0].toString());
        assertEquals("Actualización sobre tu solicitud de adopción", mensaje.getSubject());
        String contenido = contenidoComoTexto(mensaje.getContent());
        assertTrue(contenido.contains("https://refugio.test/inicio"));
        assertTrue(contenido.contains("en esta ocasión no podremos avanzar"));
        assertTrue(contenido.contains("no resta valor al cariño y compromiso"));
        verify(mailSender).send(mensaje);
    }

    @Test
    void noEnviaAvisoDeRechazoSiLaSolicitudNoEstaRechazada() {
        Cita cita = crearCitaPreaprobada();
        when(citaRepository.findWithUsuarioAndPerroByIdCita(45)).thenReturn(Optional.of(cita));

        assertThrows(IllegalStateException.class,
            () -> notificacionService.enviarAvisoRechazo(45));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void noEnviaNotificacionSiLaCitaNoEstaPreaprobada() {
        Cita cita = crearCitaPreaprobada();
        cita.setEstado(Cita.EstadoCita.confirmada);
        when(citaRepository.findWithUsuarioAndPerroByIdCita(45)).thenReturn(Optional.of(cita));

        assertThrows(IllegalStateException.class,
            () -> notificacionService.enviarAvisoPreaprobacion(45));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void propagaFalloDelServidorCorreoParaQueElAdministradorPuedaReintentar() {
        Cita cita = crearCitaPreaprobada();
        when(citaRepository.findWithUsuarioAndPerroByIdCita(45)).thenReturn(Optional.of(cita));
        when(mailSender.createMimeMessage())
            .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("SMTP no disponible"))
            .when(mailSender).send(any(MimeMessage.class));

        assertThrows(MailSendException.class,
            () -> notificacionService.enviarAvisoPreaprobacion(45));
    }

    private Cita crearCitaPreaprobada() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setEmail("usuario@example.com");
        Perro perro = new Perro();
        perro.setNombre("Luna");
        Cita cita = new Cita();
        cita.setIdCita(45);
        cita.setUsuario(usuario);
        cita.setPerro(perro);
        cita.setEstado(Cita.EstadoCita.pre_aprobada);
        return cita;
    }

    private String contenidoComoTexto(Object contenido) throws Exception {
        if (contenido instanceof Multipart multipart) {
            StringBuilder texto = new StringBuilder();
            for (int indice = 0; indice < multipart.getCount(); indice++) {
                texto.append(contenidoComoTexto(multipart.getBodyPart(indice).getContent()));
            }
            return texto.toString();
        }
        return contenido.toString();
    }
}
