package com.refugio.nueva_vida.proyecto_de_aula.service;

import com.refugio.nueva_vida.proyecto_de_aula.model.Cita;
import com.refugio.nueva_vida.proyecto_de_aula.model.Perro;
import com.refugio.nueva_vida.proyecto_de_aula.model.Usuario;
import com.refugio.nueva_vida.proyecto_de_aula.repository.CitaRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.Properties;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NotificacionCitaServiceTests {

    private static final String RESEND_API_KEY = "re_test_key";

    private JavaMailSender mailSender;
    private CitaRepository citaRepository;
    private NotificacionCitaService notificacionService;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        mailSender = mock(JavaMailSender.class);
        citaRepository = mock(CitaRepository.class);
        notificacionService = new NotificacionCitaService(
            mailSender, builder, citaRepository, "notificaciones@refugio.test", "https://refugio.test",
            "resend", RESEND_API_KEY, false);
    }

    @Test
    void enviaCorreoDePreaprobacionPorResendConElEnlaceParaElegirHorario() {
        when(citaRepository.findWithUsuarioAndPerroByIdCita(45)).thenReturn(Optional.of(crearCitaPreaprobada()));
        expectAccepted("¡Buenas noticias! Tu solicitud fue pre-aprobada")
            .andExpect(jsonPath("$.text").value(containsString("https://refugio.test/cita/45/elegir-horario")))
            .andExpect(jsonPath("$.text").value(containsString("Esta preaprobación te permite agendar una visita")))
            .andExpect(jsonPath("$.html").value(containsString("¡Tenemos buenas noticias!")))
            .andRespond(withSuccess("{\"id\":\"re_test_email\"}", MediaType.APPLICATION_JSON));

        notificacionService.enviarAvisoPreaprobacion(45);

        server.verify();
    }

    @Test
    void enviaAvisoDeRechazoConMensajeEmpaticoYEnlaceAlRefugio() {
        Cita cita = crearCitaPreaprobada();
        cita.setEstado(Cita.EstadoCita.rechazada);
        when(citaRepository.findWithUsuarioAndPerroByIdCita(45)).thenReturn(Optional.of(cita));
        expectAccepted("Actualización sobre tu solicitud de adopción")
            .andExpect(jsonPath("$.html").value(containsString("https://refugio.test/inicio")))
            .andExpect(jsonPath("$.text").value(containsString("no resta valor al cariño y compromiso")))
            .andRespond(withSuccess("{\"id\":\"re_test_email\"}", MediaType.APPLICATION_JSON));

        notificacionService.enviarAvisoRechazo(45);

        server.verify();
    }

    @Test
    void enviaAvisoDeRecepcionCuandoLaSolicitudQuedaPendienteDeRevision() {
        Cita cita = crearCitaPreaprobada();
        cita.setEstado(Cita.EstadoCita.en_espera);
        when(citaRepository.findWithUsuarioAndPerroByIdCita(45)).thenReturn(Optional.of(cita));
        expectAccepted("Recibimos tu solicitud de adopción")
            .andExpect(jsonPath("$.text").value(containsString("El equipo del refugio la revisará")))
            .andExpect(jsonPath("$.text").value(containsString("https://refugio.test/perfil")))
            .andExpect(jsonPath("$.text").value(containsString("aún no está aprobada")))
            .andRespond(withSuccess("{\"id\":\"re_test_email\"}", MediaType.APPLICATION_JSON));

        notificacionService.enviarAvisoRevisionHumana(45);

        server.verify();
    }

    @Test
    void noEnviaAvisoDeRechazoSiLaSolicitudNoEstaRechazada() {
        when(citaRepository.findWithUsuarioAndPerroByIdCita(45)).thenReturn(Optional.of(crearCitaPreaprobada()));

        assertThrows(IllegalStateException.class,
            () -> notificacionService.enviarAvisoRechazo(45));

        server.verify();
    }

    @Test
    void noEnviaAvisoDeRevisionSiLaSolicitudYaNoEstaPendiente() {
        when(citaRepository.findWithUsuarioAndPerroByIdCita(45)).thenReturn(Optional.of(crearCitaPreaprobada()));

        assertThrows(IllegalStateException.class,
            () -> notificacionService.enviarAvisoRevisionHumana(45));

        server.verify();
    }

    @Test
    void noEnviaNotificacionSiLaCitaNoEstaPreaprobada() {
        Cita cita = crearCitaPreaprobada();
        cita.setEstado(Cita.EstadoCita.confirmada);
        when(citaRepository.findWithUsuarioAndPerroByIdCita(45)).thenReturn(Optional.of(cita));

        assertThrows(IllegalStateException.class,
            () -> notificacionService.enviarAvisoPreaprobacion(45));

        server.verify();
    }

    @Test
    void propagaRechazoDeResendParaQueElAdministradorPuedaReintentar() {
        when(citaRepository.findWithUsuarioAndPerroByIdCita(45)).thenReturn(Optional.of(crearCitaPreaprobada()));
        server.expect(requestTo("https://api.resend.com/emails"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(MailSendException.class,
            () -> notificacionService.enviarAvisoPreaprobacion(45));

        server.verify();
    }

    @Test
    void informaSiFaltaLaClaveApiSinIntentarConectarse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer noUsado = MockRestServiceServer.bindTo(builder).build();
        NotificacionCitaService servicioSinClave = new NotificacionCitaService(
            mailSender, builder, citaRepository, "notificaciones@refugio.test", "https://refugio.test",
            "resend", "", false);
        when(citaRepository.findWithUsuarioAndPerroByIdCita(45)).thenReturn(Optional.of(crearCitaPreaprobada()));

        assertThrows(MailSendException.class, () -> servicioSinClave.enviarAvisoPreaprobacion(45));

        noUsado.verify();
    }

    @Test
    void conservaElEnvioSmtpParaLaDemostracionLocal() {
        RestClient.Builder builder = RestClient.builder();
        NotificacionCitaService servicioSmtp = new NotificacionCitaService(
            mailSender, builder, citaRepository, "notificaciones@refugio.test", "https://refugio.test",
            "smtp", "", false);
        Cita cita = crearCitaPreaprobada();
        MimeMessage mensaje = new MimeMessage(Session.getInstance(new Properties()));
        when(citaRepository.findWithUsuarioAndPerroByIdCita(45)).thenReturn(Optional.of(cita));
        when(mailSender.createMimeMessage()).thenReturn(mensaje);

        servicioSmtp.enviarAvisoPreaprobacion(45);

        verify(mailSender).send(mensaje);
    }

    private ResponseActions expectAccepted(String subject) {
        return server.expect(requestTo("https://api.resend.com/emails"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer " + RESEND_API_KEY))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.from").value("notificaciones@refugio.test"))
            .andExpect(jsonPath("$.to[0]").value("usuario@example.com"))
            .andExpect(jsonPath("$.subject").value(subject));
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
}
