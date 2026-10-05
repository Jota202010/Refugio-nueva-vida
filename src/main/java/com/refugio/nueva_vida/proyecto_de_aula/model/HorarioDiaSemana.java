package com.refugio.nueva_vida.proyecto_de_aula.model;

import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "horario_dia_semana", uniqueConstraints =
    @UniqueConstraint(name = "uq_horario_dia_semana", columnNames = "dia_semana"))
public class HorarioDiaSemana {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dia")
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 16)
    private DayOfWeek dia;

    @Column(name = "manana_inicio")
    private LocalTime mananaInicio;

    @Column(name = "manana_fin")
    private LocalTime mananaFin;

    @Column(name = "tarde_inicio")
    private LocalTime tardeInicio;

    @Column(name = "tarde_fin")
    private LocalTime tardeFin;

    public Integer getId() { return id; }
    public DayOfWeek getDia() { return dia; }
    public void setDia(DayOfWeek dia) { this.dia = dia; }
    public LocalTime getMananaInicio() { return mananaInicio; }
    public void setMananaInicio(LocalTime mananaInicio) { this.mananaInicio = mananaInicio; }
    public LocalTime getMananaFin() { return mananaFin; }
    public void setMananaFin(LocalTime mananaFin) { this.mananaFin = mananaFin; }
    public LocalTime getTardeInicio() { return tardeInicio; }
    public void setTardeInicio(LocalTime tardeInicio) { this.tardeInicio = tardeInicio; }
    public LocalTime getTardeFin() { return tardeFin; }
    public void setTardeFin(LocalTime tardeFin) { this.tardeFin = tardeFin; }
}
