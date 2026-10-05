package com.refugio.nueva_vida.proyecto_de_aula.web;

import java.util.ArrayList;
import java.util.List;

public class ConfiguracionHorarioForm {
    private Integer duracionMinutos;
    private Integer profesionalesDisponibles;
    private List<HorarioDiaForm> dias = new ArrayList<>();

    public Integer getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }
    public Integer getProfesionalesDisponibles() { return profesionalesDisponibles; }
    public void setProfesionalesDisponibles(Integer profesionalesDisponibles) {
        this.profesionalesDisponibles = profesionalesDisponibles;
    }
    public List<HorarioDiaForm> getDias() { return dias; }
    public void setDias(List<HorarioDiaForm> dias) { this.dias = dias; }
}
