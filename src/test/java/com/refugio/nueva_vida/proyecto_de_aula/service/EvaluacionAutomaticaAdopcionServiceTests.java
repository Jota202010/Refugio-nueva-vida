package com.refugio.nueva_vida.proyecto_de_aula.service;

import com.refugio.nueva_vida.proyecto_de_aula.model.Cita;
import com.refugio.nueva_vida.proyecto_de_aula.model.Cita.EstadoEvaluacionIa;
import com.refugio.nueva_vida.proyecto_de_aula.model.Perro;
import com.refugio.nueva_vida.proyecto_de_aula.web.EvaluacionAdopcionRespuesta;
import com.refugio.nueva_vida.proyecto_de_aula.web.EvaluacionAdopcionSolicitud;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EvaluacionAutomaticaAdopcionServiceTests {

    private GeminiChatClient geminiChatClient;
    private EvaluacionAutomaticaAdopcionService service;

    @BeforeEach
    void setUp() {
        geminiChatClient = mock(GeminiChatClient.class);
        service = new EvaluacionAutomaticaAdopcionService(geminiChatClient);
    }

    @Test
    void sinConsentimientoDejaLaSolicitudParaRevisionHumanaSinLlamarGemini() {
        Cita cita = solicitudValida();
        cita.setAceptaEvaluacionIa(false);

        var result = service.evaluar(cita);

        assertEquals(EstadoEvaluacionIa.sin_consentimiento, result.estado());
        verifyNoInteractions(geminiChatClient);
    }

    @Test
    void condicionesNoElegiblesSeEnvianARevisionHumanaSinLlamarGemini() {
        Cita cita = solicitudValida();
        cita.setMascotasActual(true);

        var result = service.evaluar(cita);

        assertEquals(EstadoEvaluacionIa.revision_humana, result.estado());
        verifyNoInteractions(geminiChatClient);
    }

    @Test
    void alquilerSinPermisoConfirmadoRequiereRevisionHumana() {
        Cita cita = solicitudValida();
        cita.setPropiedad(Cita.Propiedad.alquilada);
        cita.setPermitenMascotas(Cita.PermitenMascotas.no);

        var result = service.evaluar(cita);

        assertEquals(EstadoEvaluacionIa.revision_humana, result.estado());
        verifyNoInteractions(geminiChatClient);
    }

    @Test
    void tiempoSoloDeCuatroHorasOMasRequiereRevisionHumana() {
        Cita cita = solicitudValida();
        cita.setHorasSolo("4-6 horas");

        var result = service.evaluar(cita);

        assertEquals(EstadoEvaluacionIa.revision_humana, result.estado());
        verifyNoInteractions(geminiChatClient);
    }

    @Test
    void unaRespuestaNegativaNuncaSeConvierteEnRechazoAutomatico() {
        Cita cita = solicitudValida();
        cita.setCubreEmergencias(false);

        var result = service.evaluar(cita);

        assertEquals(EstadoEvaluacionIa.revision_humana, result.estado());
        verifyNoInteractions(geminiChatClient);
    }

    @Test
    void soloUnaRespuestaValidaDeGeminiPuedePreaprobarUnCasoElegible() {
        Cita cita = solicitudValida();
        when(geminiChatClient.evaluarSolicitudAdopcion(any()))
            .thenReturn(new EvaluacionAdopcionRespuesta(
                "PRE_APPROVE", "No se observan incompatibilidades claras."));

        var result = service.evaluar(cita);

        assertEquals(EstadoEvaluacionIa.preaprobada, result.estado());
        verify(geminiChatClient).evaluarSolicitudAdopcion(any(EvaluacionAdopcionSolicitud.class));
    }

    @Test
    void resultadoInciertoDeGeminiDejaLaSolicitudEnRevision() {
        Cita cita = solicitudValida();
        when(geminiChatClient.evaluarSolicitudAdopcion(any()))
            .thenReturn(new EvaluacionAdopcionRespuesta("HUMAN_REVIEW", "Se necesita revisar compatibilidad."));

        var result = service.evaluar(cita);

        assertEquals(EstadoEvaluacionIa.revision_humana, result.estado());
    }

    @Test
    void fallaDelProveedorQuedaRegistradaComoRevisionSinPreaprobar() {
        Cita cita = solicitudValida();
        when(geminiChatClient.evaluarSolicitudAdopcion(any()))
            .thenThrow(new GeminiChatClient.ChatProviderException("No disponible."));

        var result = service.evaluar(cita);

        assertEquals(EstadoEvaluacionIa.error, result.estado());
        verify(geminiChatClient).evaluarSolicitudAdopcion(any(EvaluacionAdopcionSolicitud.class));
    }

    private Cita solicitudValida() {
        Perro perro = new Perro();
        perro.setEdad("2 años");
        perro.setSexo(Perro.Sexo.Macho);
        perro.setSociabilidad(Perro.Sociabilidad.ALTA);
        perro.setDescripcion("Perro tranquilo y sociable.");

        Cita cita = new Cita();
        cita.setIdCita(31);
        cita.setPerro(perro);
        cita.setAceptaEvaluacionIa(true);
        cita.setTipoVivienda(Cita.TipoVivienda.casa);
        cita.setPropiedad(Cita.Propiedad.propia);
        cita.setPermitenMascotas(Cita.PermitenMascotas.no_aplica);
        cita.setTodosAcuerdo(true);
        cita.setMascotasActual(false);
        cita.setHorasSolo("2-4 horas");
        cita.setPuedePasear(true);
        cita.setCubreVet(true);
        cita.setCubreEmergencias(true);
        cita.setCondNoAbandono(true);
        cita.setCondSeguimiento(true);
        cita.setCondEvaluacion(true);
        return cita;
    }
}
