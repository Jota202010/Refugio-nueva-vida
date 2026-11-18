package com.refugio.nueva_vida.proyecto_de_aula.service;

import com.refugio.nueva_vida.proyecto_de_aula.model.Cita;
import com.refugio.nueva_vida.proyecto_de_aula.model.Cita.EstadoEvaluacionIa;
import com.refugio.nueva_vida.proyecto_de_aula.model.HistorialEstado;
import com.refugio.nueva_vida.proyecto_de_aula.model.Perro;
import com.refugio.nueva_vida.proyecto_de_aula.model.Usuario;
import com.refugio.nueva_vida.proyecto_de_aula.repository.CitaRepository;
import com.refugio.nueva_vida.proyecto_de_aula.repository.HistorialEstadoRepository;
import com.refugio.nueva_vida.proyecto_de_aula.repository.PerroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CitaServiceAdopcionTests {

    private CitaRepository citaRepository;
    private HistorialEstadoRepository historialRepository;
    private PerroRepository perroRepository;
    private CitaService citaService;

    @BeforeEach
    void setUp() {
        citaRepository = mock(CitaRepository.class);
        historialRepository = mock(HistorialEstadoRepository.class);
        perroRepository = mock(PerroRepository.class);
        citaService = new CitaService(
            citaRepository,
            mock(HorarioService.class),
            perroRepository,
            historialRepository
        );
    }

    @Test
    void confirmarAdopcionActualizaEstadoYEnlazaHistorialConCitaYAdoptante() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7);
        Perro perro = new Perro();
        perro.setIdPerro(12);
        perro.setEstadoPublicacion(Perro.EstadoPublicacion.EN_PROCESO);
        Cita cita = new Cita();
        cita.setIdCita(34);
        cita.setUsuario(usuario);
        cita.setPerro(perro);
        cita.setEstado(Cita.EstadoCita.confirmada);
        cita.setFechaCita(LocalDate.now().plusDays(1));
        cita.setHoraCita(LocalTime.of(10, 30));

        when(citaRepository.findByIdForUpdate(34)).thenReturn(Optional.of(cita));
        when(historialRepository.existsByCita_IdCita(34)).thenReturn(false);
        when(perroRepository.findByIdForUpdate(12)).thenReturn(Optional.of(perro));

        citaService.confirmarAdopcion(34);

        assertEquals(Perro.EstadoPublicacion.ADOPTADO, perro.getEstadoPublicacion());
        ArgumentCaptor<HistorialEstado> historialCaptor = ArgumentCaptor.forClass(HistorialEstado.class);
        verify(historialRepository).save(historialCaptor.capture());
        HistorialEstado historial = historialCaptor.getValue();
        assertEquals(perro, historial.getPerro());
        assertEquals(cita, historial.getCita());
        assertEquals(usuario, historial.getAdoptante());
        assertEquals(Perro.EstadoPublicacion.EN_PROCESO, historial.getEstadoAnterior());
        assertEquals(Perro.EstadoPublicacion.ADOPTADO, historial.getEstadoNuevo());
        assertEquals(HistorialEstado.Origen.ADMIN, historial.getOrigen());
    }

    @Test
    void confirmarAdopcionRechazaCitasSinHorarioConfirmado() {
        Cita cita = new Cita();
        cita.setIdCita(34);
        cita.setEstado(Cita.EstadoCita.pre_aprobada);
        when(citaRepository.findByIdForUpdate(34)).thenReturn(Optional.of(cita));

        assertThrows(IllegalStateException.class, () -> citaService.confirmarAdopcion(34));

        verifyNoInteractions(historialRepository, perroRepository);
    }

    @Test
    void confirmarAdopcionRechazaPerrosYaAdoptados() {
        Perro perro = new Perro();
        perro.setIdPerro(12);
        perro.setEstadoPublicacion(Perro.EstadoPublicacion.ADOPTADO);
        Cita cita = new Cita();
        cita.setIdCita(34);
        cita.setPerro(perro);
        cita.setEstado(Cita.EstadoCita.confirmada);
        cita.setFechaCita(LocalDate.now());
        cita.setHoraCita(LocalTime.of(10, 30));

        when(citaRepository.findByIdForUpdate(34)).thenReturn(Optional.of(cita));
        when(historialRepository.existsByCita_IdCita(34)).thenReturn(false);
        when(perroRepository.findByIdForUpdate(12)).thenReturn(Optional.of(perro));

        assertThrows(IllegalStateException.class, () -> citaService.confirmarAdopcion(34));

        verify(historialRepository, never()).save(any());
        verify(perroRepository, never()).save(any());
    }

    @Test
    void registraPreaprobacionAutomaticaSinAtribuirlaAUnAdministrador() {
        Cita cita = new Cita();
        cita.setIdCita(34);
        cita.setEstado(Cita.EstadoCita.en_espera);
        when(citaRepository.findByIdForUpdate(34)).thenReturn(Optional.of(cita));
        when(citaRepository.save(cita)).thenReturn(cita);

        Cita result = citaService.registrarEvaluacionAutomatica(
            34, EstadoEvaluacionIa.preaprobada, "Sin incompatibilidades evidentes.");

        assertEquals(Cita.EstadoCita.pre_aprobada, result.getEstado());
        assertEquals(EstadoEvaluacionIa.preaprobada, result.getEvaluacionIaEstado());
        assertEquals("Sin incompatibilidades evidentes.", result.getEvaluacionIaMotivo());
        assertNull(result.getAdmin());
        assertNotNull(result.getEvaluacionIaFecha());
    }

    @Test
    void guardarNormalizaLosCamposQueDependenDeRespuestasCondicionales() {
        Usuario usuario = new Usuario();
        Perro perro = new Perro();
        perro.setIdPerro(12);
        perro.setEstadoPublicacion(Perro.EstadoPublicacion.PUBLICADO);
        Cita cita = new Cita();
        cita.setUsuario(usuario);
        cita.setPerro(perro);
        cita.setPropiedad(Cita.Propiedad.propia);
        cita.setPermitenMascotas(Cita.PermitenMascotas.no);
        cita.setNumPersonas(1);
        cita.setPerrosAntes(false);
        cita.setMascotasAnteriores("Respuesta que ya no aplica");

        when(perroRepository.findById(12)).thenReturn(Optional.of(perro));
        when(citaRepository.findByUsuario(usuario)).thenReturn(List.of());
        when(citaRepository.save(cita)).thenReturn(cita);

        Cita guardada = citaService.guardar(cita);

        assertEquals(Cita.PermitenMascotas.no_aplica, guardada.getPermitenMascotas());
        assertEquals(Boolean.TRUE, guardada.getTodosAcuerdo());
        assertNull(guardada.getMascotasAnteriores());
    }
}
