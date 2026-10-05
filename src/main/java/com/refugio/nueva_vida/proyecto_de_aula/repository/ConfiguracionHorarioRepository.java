package com.refugio.nueva_vida.proyecto_de_aula.repository;

import com.refugio.nueva_vida.proyecto_de_aula.model.ConfiguracionHorario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConfiguracionHorarioRepository extends JpaRepository<ConfiguracionHorario, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConfiguracionHorario c where c.id = :id")
    Optional<ConfiguracionHorario> findByIdForUpdate(@Param("id") Integer id);
}
