package com.refugio.nueva_vida.proyecto_de_aula.web;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class HorarioDiaForm {
    private DayOfWeek dia;
    private String nombre;
    private boolean habilitado;
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime mananaInicio;
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime mananaFin;
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime tardeInicio;
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime tardeFin;

    public DayOfWeek getDia() { return dia; }
    public void setDia(DayOfWeek dia) { this.dia = dia; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public boolean isHabilitado() { return habilitado; }
    public void setHabilitado(boolean habilitado) { this.habilitado = habilitado; }
    public LocalTime getMananaInicio() { return mananaInicio; }
    public void setMananaInicio(LocalTime mananaInicio) { this.mananaInicio = mananaInicio; }
    public LocalTime getMananaFin() { return mananaFin; }
    public void setMananaFin(LocalTime mananaFin) { this.mananaFin = mananaFin; }
    public LocalTime getTardeInicio() { return tardeInicio; }
    public void setTardeInicio(LocalTime tardeInicio) { this.tardeInicio = tardeInicio; }
    public LocalTime getTardeFin() { return tardeFin; }
    public void setTardeFin(LocalTime tardeFin) { this.tardeFin = tardeFin; }
}
