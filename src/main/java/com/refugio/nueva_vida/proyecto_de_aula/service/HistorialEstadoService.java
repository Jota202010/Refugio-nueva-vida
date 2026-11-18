package com.refugio.nueva_vida.proyecto_de_aula.service;

import com.refugio.nueva_vida.proyecto_de_aula.model.HistorialEstado;
import com.refugio.nueva_vida.proyecto_de_aula.model.Perro;
import com.refugio.nueva_vida.proyecto_de_aula.model.Usuario;
import com.refugio.nueva_vida.proyecto_de_aula.repository.HistorialEstadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class HistorialEstadoService {

    private final HistorialEstadoRepository repo;

    public HistorialEstadoService(HistorialEstadoRepository repo) {
        this.repo = repo;
    }

    /**
     * Registra un cambio de estado solo si el estado realmente cambió.
     * Si estadoAnterior == estadoNuevo, no hace nada.
     */
    public void registrar(Perro perro,
                          Perro.EstadoPublicacion estadoAnterior,
                          Perro.EstadoPublicacion estadoNuevo,
                          HistorialEstado.Origen origen) {
        registrar(perro, estadoAnterior, estadoNuevo, origen, null);
    }

    public void registrar(Perro perro,
                          Perro.EstadoPublicacion estadoAnterior,
                          Perro.EstadoPublicacion estadoNuevo,
                          HistorialEstado.Origen origen,
                          Usuario adoptante) {

        if (estadoAnterior == estadoNuevo) return; // sin cambio real, no registrar

        HistorialEstado registro = new HistorialEstado(
            perro, estadoAnterior, estadoNuevo, origen, null, adoptante);
        repo.save(registro);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> adoptanteDeUltimaAdopcion(Perro perro) {
        return repo.findFirstByPerroAndEstadoNuevoOrderByFechaCambioDesc(
                perro, Perro.EstadoPublicacion.ADOPTADO)
            .map(HistorialEstado::getAdoptante);
    }

    /** Lista el historial de un perro del más reciente al más antiguo */
    @Transactional(readOnly = true)
    public List<HistorialEstado> historialDePerro(Perro perro) {
        return repo.findByPerroOrderByFechaCambioDesc(perro);
    }
}
