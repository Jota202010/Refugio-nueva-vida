package com.refugio.nueva_vida.proyecto_de_aula.web;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public class HorarioExcepcionForm {
    private LocalDate fecha;
    private boolean cerrado;
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime mananaInicio;
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime mananaFin;
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime tardeInicio;
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime tardeFin;

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
