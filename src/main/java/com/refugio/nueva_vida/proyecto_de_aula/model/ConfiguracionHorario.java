package com.refugio.nueva_vida.proyecto_de_aula.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "configuracion_horario")
public class ConfiguracionHorario {

    @Id
    private Integer id = 1;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos = 30;

    @Column(name = "profesionales_disponibles", nullable = false)
    private Integer profesionalesDisponibles = 1;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }
    public Integer getProfesionalesDisponibles() { return profesionalesDisponibles; }
    public void setProfesionalesDisponibles(Integer profesionalesDisponibles) {
        this.profesionalesDisponibles = profesionalesDisponibles;
    }
}
