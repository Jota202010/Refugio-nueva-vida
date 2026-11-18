package com.refugio.nueva_vida.proyecto_de_aula.web;

public record EvaluacionAdopcionSolicitud(
    Aplicacion application,
    PerroPublico dog
) {
    public record Aplicacion(
        String tipoVivienda,
        String propiedad,
        String permitenMascotas,
        boolean todosAcuerdo,
        boolean mascotasActuales,
        String horasSolo,
        boolean puedePasear,
        boolean cubreVeterinaria,
        boolean cubreEmergencias,
        boolean aceptaNoAbandono,
        boolean aceptaSeguimiento,
        boolean aceptaEvaluacion
    ) {}

    public record PerroPublico(
        String edad,
        String sexo,
        String sociabilidad,
        String descripcion
    ) {}
}
