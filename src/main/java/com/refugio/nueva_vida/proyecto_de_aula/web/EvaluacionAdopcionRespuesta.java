package com.refugio.nueva_vida.proyecto_de_aula.web;

public record EvaluacionAdopcionRespuesta(String decision, String summary) {
    public enum Decision {
        PRE_APPROVE,
        HUMAN_REVIEW
    }
}
