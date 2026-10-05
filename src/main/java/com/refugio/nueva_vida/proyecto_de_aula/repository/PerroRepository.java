package com.refugio.nueva_vida.proyecto_de_aula.repository;

import com.refugio.nueva_vida.proyecto_de_aula.model.Perro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PerroRepository extends JpaRepository<Perro, Integer> {

    // Animales visibles en el listado público (reemplaza findByAdoptadoFalseAndListaParaAdoptarTrue)
    List<Perro> findByEstadoPublicacion(Perro.EstadoPublicacion estadoPublicacion);

    // Buscar por estado de origen
    List<Perro> findByEstado(Perro.Estado estado);

    // Buscar por nombre (case-insensitive)
    List<Perro> findByNombreContainingIgnoreCase(String nombre);
    Optional<Perro> findByNombreIgnoreCase(String nombre);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Perro p where p.idPerro = :id")
    Optional<Perro> findByIdForUpdate(@Param("id") Integer id);
}
