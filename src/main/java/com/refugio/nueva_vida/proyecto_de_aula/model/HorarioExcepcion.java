package com.refugio.nueva_vida.proyecto_de_aula.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "horario_excepcion", uniqueConstraints =
    @UniqueConstraint(name = "uq_horario_excepcion_fecha", columnNames = "fecha"))
public class HorarioExcepcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_excepcion")
    private Integer id;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "cerrado", nullable = false)
    private boolean cerrado;

    @Column(name = "manana_inicio")
    private LocalTime mananaInicio;

    @Column(name = "manana_fin")
    private LocalTime mananaFin;

    @Column(name = "tarde_inicio")
    private LocalTime tardeInicio;

    @Column(name = "tarde_fin")
    private LocalTime tardeFin;

    public Integer getId() { return id; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public boolean isCerrado() { return cerrado; }
    public void setCerrado(boolean cerrado) { this.cerrado = cerrado; }
    public LocalTime getMananaInicio() { return mananaInicio; }
    public void setMananaInicio(LocalTime mananaInicio) { this.mananaInicio = mananaInicio; }
    public LocalTime getMananaFin() { return mananaFin; }
    public void setMananaFin(LocalTime mananaFin) { this.mananaFin = mananaFin; }
    public LocalTime getTardeInicio() { return tardeInicio; }
    public void setTardeInicio(LocalTime tardeInicio) { this.tardeInicio = tardeInicio; }
    public LocalTime getTardeFin() { return tardeFin; }
    public void setTardeFin(LocalTime tardeFin) { this.tardeFin = tardeFin; }
}
