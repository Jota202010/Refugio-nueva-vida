package com.refugio.nueva_vida.proyecto_de_aula.controller;

import com.refugio.nueva_vida.proyecto_de_aula.model.Cita;
import com.refugio.nueva_vida.proyecto_de_aula.model.Perro;
import com.refugio.nueva_vida.proyecto_de_aula.model.Usuario;
import com.refugio.nueva_vida.proyecto_de_aula.service.CitaService;
import com.refugio.nueva_vida.proyecto_de_aula.service.EvaluacionAutomaticaAdopcionService;
import com.refugio.nueva_vida.proyecto_de_aula.service.HorarioService;
import com.refugio.nueva_vida.proyecto_de_aula.service.NotificacionCitaService;
import com.refugio.nueva_vida.proyecto_de_aula.service.PerroService;
import com.refugio.nueva_vida.proyecto_de_aula.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CitaControllerTests {

    @Test
    void enviaNotificacionCuandoLaSolicitudQuedaPendienteDeRevisionHumana() {
        CitaService citaService = mock(CitaService.class);
        PerroService perroService = mock(PerroService.class);
        UsuarioService usuarioService = mock(UsuarioService.class);
        HorarioService horarioService = mock(HorarioService.class);
        EvaluacionAutomaticaAdopcionService evaluacionService =
            mock(EvaluacionAutomaticaAdopcionService.class);
        NotificacionCitaService notificacionService = mock(NotificacionCitaService.class);
        UserDetails userDetails = mock(UserDetails.class);

        Perro perro = new Perro();
        perro.setIdPerro(8);
        perro.setEstadoPublicacion(Perro.EstadoPublicacion.PUBLICADO);
        Usuario usuario = new Usuario();
        Cita guardada = new Cita();
        guardada.setIdCita(31);
        guardada.setEstado(Cita.EstadoCita.en_espera);
        var resultado = new EvaluacionAutomaticaAdopcionService.Resultado(
            Cita.EstadoEvaluacionIa.revision_humana, "Requiere revisión humana.");

        when(perroService.buscarPorId(8)).thenReturn(Optional.of(perro));
        when(userDetails.getUsername()).thenReturn("adoptante");
        when(usuarioService.buscarPorUsername("adoptante")).thenReturn(Optional.of(usuario));
        when(citaService.guardar(any(Cita.class))).thenReturn(guardada);
        when(evaluacionService.evaluar(guardada)).thenReturn(resultado);
        when(citaService.registrarEvaluacionAutomatica(
            31, resultado.estado(), resultado.motivo())).thenReturn(guardada);

        CitaController controller = new CitaController(
            citaService, perroService, usuarioService, horarioService, evaluacionService, notificacionService);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.enviarSolicitud(8, new Cita(), userDetails, redirectAttributes);

        assertEquals("redirect:/perfil", view);
        assertTrue(redirectAttributes.getFlashAttributes().get("mensajeExito").toString()
            .contains("Te avisaremos por correo"));
        verify(notificacionService).enviarAvisoRevisionHumana(31);
    }
}
