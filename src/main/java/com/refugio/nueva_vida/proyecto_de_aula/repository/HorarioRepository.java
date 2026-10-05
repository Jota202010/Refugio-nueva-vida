package com.refugio.nueva_vida.proyecto_de_aula.repository;

import com.refugio.nueva_vida.proyecto_de_aula.model.HorarioDisponible;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface HorarioRepository extends JpaRepository<HorarioDisponible, Integer> {

    // Todos los horarios de una fecha específica
    List<HorarioDisponible> findByFechaOrderByHoraAsc(LocalDate fecha);

    List<HorarioDisponible> findByFechaAndOcupadoTrue(LocalDate fecha);
}
