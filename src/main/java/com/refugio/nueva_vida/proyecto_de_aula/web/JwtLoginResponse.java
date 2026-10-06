package com.refugio.nueva_vida.proyecto_de_aula.web;

public record JwtLoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn
) {}
