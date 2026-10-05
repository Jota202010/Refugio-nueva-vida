package com.refugio.nueva_vida.proyecto_de_aula.service;

import com.refugio.nueva_vida.proyecto_de_aula.model.*;
import com.refugio.nueva_vida.proyecto_de_aula.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HorarioServiceTests {

    private HorarioRepository horarioRepository;
    private ConfiguracionHorarioRepository configuracionRepository;
    private HorarioDiaSemanaRepository diaSemanaRepository;
    private HorarioExcepcionRepository excepcionRepository;
    private CitaRepository citaRepository;
    private HorarioService horarioService;
    private ConfiguracionHorario configuracion;
    private LocalDate fechaDisponible;

    @BeforeEach
    void setUp() {
        horarioRepository = mock(HorarioRepository.class);
        configuracionRepository = mock(ConfiguracionHorarioRepository.class);
        diaSemanaRepository = mock(HorarioDiaSemanaRepository.class);
        excepcionRepository = mock(HorarioExcepcionRepository.class);
        citaRepository = mock(CitaRepository.class);
        horarioService = new HorarioService(horarioRepository, configuracionRepository,
            diaSemanaRepository, excepcionRepository, citaRepository);

        fechaDisponible = LocalDate.now().plusDays(1);
        configuracion = new ConfiguracionHorario();
        configuracion.setId(1);
        configuracion.setDuracionMinutos(30);
        configuracion.setProfesionalesDisponibles(1);

        when(configuracionRepository.findById(1)).thenReturn(Optional.of(configuracion));
        when(configuracionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(configuracion));
        when(diaSemanaRepository.count()).thenReturn(7L);
        when(diaSemanaRepository.findAll()).thenReturn(semanaConUnDiaAbierto());
        when(excepcionRepository.findAll()).thenReturn(List.of());
        when(horarioRepository.findByFechaAndOcupadoTrue(fechaDisponible)).thenReturn(List.of());
    }

    @Test
    void generaTurnosConLaDuracionConfiguradaYRespetaLaCapacidad() {
        HorarioDisponible ocupado = new HorarioDisponible(fechaDisponible, LocalTime.of(8, 0));
        ocupado.setOcupado(true);
        ocupado.setDuracionMinutos(30);
        when(horarioRepository.findByFechaAndOcupadoTrue(fechaDisponible)).thenReturn(List.of(ocupado));

        List<HorarioDisponible> disponibles = horarioService.generarSlotsDisponibles(1);

        assertEquals(3, disponibles.size());
        assertTrue(disponibles.stream().noneMatch(slot -> slot.getHora().equals(LocalTime.of(8, 0))));
        assertTrue(disponibles.stream().allMatch(slot -> slot.getDuracionMinutos() == 30));
    }

    @Test
    void reservarRechazaUnTurnoQueYaAlcanzoSuCapacidad() {
        HorarioDisponible ocupado = new HorarioDisponible(fechaDisponible, LocalTime.of(8, 0));
        ocupado.setOcupado(true);
        ocupado.setDuracionMinutos(30);
        when(horarioRepository.findByFechaAndOcupadoTrue(fechaDisponible)).thenReturn(List.of(ocupado));

        assertThrows(IllegalStateException.class,
            () -> horarioService.ocuparPorFechaHora(fechaDisponible, LocalTime.of(8, 0), new Cita()));

        verify(horarioRepository, never()).save(any(HorarioDisponible.class));
    }

    @Test
    void permiteReservarElMismoTurnoMientrasQuedeCapacidad() {
        configuracion.setProfesionalesDisponibles(2);
        HorarioDisponible ocupado = new HorarioDisponible(fechaDisponible, LocalTime.of(8, 0));
        ocupado.setOcupado(true);
        ocupado.setDuracionMinutos(30);
        when(horarioRepository.findByFechaAndOcupadoTrue(fechaDisponible)).thenReturn(List.of(ocupado));

        horarioService.ocuparPorFechaHora(fechaDisponible, LocalTime.of(8, 0), new Cita());

        verify(horarioRepository).save(argThat(horario ->
            horario.getFecha().equals(fechaDisponible)
                && horario.getHora().equals(LocalTime.of(8, 0))
                && horario.getDuracionMinutos() == 30));
    }

    @Test
    void rechazaOtraCitaSolapadaDelMismoUsuario() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(18);
        Cita citaExistente = new Cita();
        citaExistente.setUsuario(usuario);
        citaExistente.setEstado(Cita.EstadoCita.confirmada);
        citaExistente.setFechaCita(fechaDisponible);
        citaExistente.setHoraCita(LocalTime.of(8, 0));
        when(citaRepository.findByUsuario(usuario)).thenReturn(List.of(citaExistente));

        Cita nuevaCita = new Cita();
        nuevaCita.setUsuario(usuario);

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> horarioService.ocuparPorFechaHora(fechaDisponible, LocalTime.of(8, 0), nuevaCita));

        assertTrue(error.getMessage().contains("otra cita confirmada"));
        verify(horarioRepository, never()).save(any(HorarioDisponible.class));
    }

    @Test
    void noConsideraLasCitasDeOtrosUsuariosUnConflictoPersonal() {
        configuracion.setProfesionalesDisponibles(2);
        HorarioDisponible horarioOcupado = new HorarioDisponible(fechaDisponible, LocalTime.of(8, 0));
        horarioOcupado.setOcupado(true);
        horarioOcupado.setDuracionMinutos(30);
        when(horarioRepository.findByFechaAndOcupadoTrue(fechaDisponible))
            .thenReturn(List.of(horarioOcupado));
        when(citaRepository.findByUsuario(any(Usuario.class))).thenReturn(List.of());

        Cita nuevaCita = new Cita();
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(20);
        nuevaCita.setUsuario(usuario);

        assertDoesNotThrow(() -> horarioService.ocuparPorFechaHora(
            fechaDisponible, LocalTime.of(8, 0), nuevaCita));
    }

    @Test
    void unaExcepcionCerradaOmiteEseDiaSinModificarLaReglaSemanal() {
        HorarioExcepcion excepcion = new HorarioExcepcion();
        excepcion.setFecha(fechaDisponible);
        excepcion.setCerrado(true);
        when(excepcionRepository.findAll()).thenReturn(List.of(excepcion));

        List<HorarioDisponible> disponibles = horarioService.generarSlotsDisponibles(1);

        assertFalse(disponibles.isEmpty());
        assertTrue(disponibles.stream().allMatch(slot -> slot.getFecha().isAfter(fechaDisponible)));
        assertEquals(fechaDisponible.getDayOfWeek(), disponibles.get(0).getFecha().getDayOfWeek());
    }

    private List<HorarioDiaSemana> semanaConUnDiaAbierto() {
        List<HorarioDiaSemana> dias = new ArrayList<>();
        for (DayOfWeek dia : DayOfWeek.values()) {
            HorarioDiaSemana horario = new HorarioDiaSemana();
            horario.setDia(dia);
            if (dia == fechaDisponible.getDayOfWeek()) {
                horario.setMananaInicio(LocalTime.of(8, 0));
                horario.setMananaFin(LocalTime.of(10, 0));
            }
            dias.add(horario);
        }
        return dias;
    }
}
