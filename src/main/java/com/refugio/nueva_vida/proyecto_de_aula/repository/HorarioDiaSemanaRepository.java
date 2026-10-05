package com.refugio.nueva_vida.proyecto_de_aula.repository;

import com.refugio.nueva_vida.proyecto_de_aula.model.HorarioDiaSemana;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface HorarioDiaSemanaRepository extends JpaRepository<HorarioDiaSemana, Integer> {
    List<HorarioDiaSemana> findAllByOrderByDiaAsc();
    Optional<HorarioDiaSemana> findByDia(DayOfWeek dia);
}
