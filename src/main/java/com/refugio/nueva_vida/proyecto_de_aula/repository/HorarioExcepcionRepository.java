package com.refugio.nueva_vida.proyecto_de_aula.repository;

import com.refugio.nueva_vida.proyecto_de_aula.model.HorarioExcepcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HorarioExcepcionRepository extends JpaRepository<HorarioExcepcion, Integer> {
    List<HorarioExcepcion> findByFechaGreaterThanEqualOrderByFechaAsc(LocalDate fecha);
    Optional<HorarioExcepcion> findByFecha(LocalDate fecha);
}
