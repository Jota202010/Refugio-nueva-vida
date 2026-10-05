package com.refugio.nueva_vida.proyecto_de_aula.service;

import com.refugio.nueva_vida.proyecto_de_aula.model.Cita;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
