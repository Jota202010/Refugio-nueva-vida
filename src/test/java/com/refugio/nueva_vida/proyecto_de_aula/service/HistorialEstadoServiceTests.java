package com.refugio.nueva_vida.proyecto_de_aula.service;

import com.refugio.nueva_vida.proyecto_de_aula.model.HistorialEstado;
import com.refugio.nueva_vida.proyecto_de_aula.model.Perro;
import com.refugio.nueva_vida.proyecto_de_aula.model.Usuario;
import com.refugio.nueva_vida.proyecto_de_aula.repository.HistorialEstadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class HistorialEstadoServiceTests {

    private HistorialEstadoRepository repository;
    private HistorialEstadoService historialEstadoService;

    @BeforeEach
    void setUp() {
        repository = mock(HistorialEstadoRepository.class);
        historialEstadoService = new HistorialEstadoService(repository);
    }

    @Test
    void buscaElAdoptanteDeLaAdopcionMasReciente() {
        Perro perro = new Perro();
        Usuario adoptante = new Usuario();
        HistorialEstado adopcion = new HistorialEstado(
            perro,
            Perro.EstadoPublicacion.PUBLICADO,
            Perro.EstadoPublicacion.ADOPTADO,
            HistorialEstado.Origen.ADMIN,
            null,
            adoptante
        );
        when(repository.findFirstByPerroAndEstadoNuevoOrderByFechaCambioDesc(
                perro, Perro.EstadoPublicacion.ADOPTADO))
            .thenReturn(Optional.of(adopcion));

        Optional<Usuario> resultado = historialEstadoService.adoptanteDeUltimaAdopcion(perro);

        assertEquals(Optional.of(adoptante), resultado);
    }

    @Test
    void registraLaDevolucionVinculadaAlAdoptanteYNoReutilizaLaCita() {
        Perro perro = new Perro();
        Usuario adoptante = new Usuario();

        historialEstadoService.registrar(
            perro,
            Perro.EstadoPublicacion.ADOPTADO,
            Perro.EstadoPublicacion.DEVUELTO,
            HistorialEstado.Origen.ADMIN,
            adoptante
        );

        ArgumentCaptor<HistorialEstado> registroCaptor = ArgumentCaptor.forClass(HistorialEstado.class);
        verify(repository).save(registroCaptor.capture());
        HistorialEstado registro = registroCaptor.getValue();
        assertEquals(perro, registro.getPerro());
        assertEquals(adoptante, registro.getAdoptante());
        assertNull(registro.getCita());
        assertEquals(Perro.EstadoPublicacion.ADOPTADO, registro.getEstadoAnterior());
        assertEquals(Perro.EstadoPublicacion.DEVUELTO, registro.getEstadoNuevo());
    }

    @Test
    void devuelveVacioSiLaUltimaAdopcionNoTieneUsuarioRegistrado() {
        Perro perro = new Perro();
        HistorialEstado adopcionSinUsuario = new HistorialEstado(
            perro,
            Perro.EstadoPublicacion.PUBLICADO,
            Perro.EstadoPublicacion.ADOPTADO,
            HistorialEstado.Origen.ADMIN,
            null,
            null
        );
        when(repository.findFirstByPerroAndEstadoNuevoOrderByFechaCambioDesc(
                perro, Perro.EstadoPublicacion.ADOPTADO))
            .thenReturn(Optional.of(adopcionSinUsuario));

        Optional<Usuario> resultado = historialEstadoService.adoptanteDeUltimaAdopcion(perro);

        assertEquals(Optional.empty(), resultado);
    }
}
