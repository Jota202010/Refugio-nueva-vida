package com.refugio.nueva_vida.proyecto_de_aula.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JwtLoginRequest(
    @NotBlank @Size(max = 150) String username,
    @NotBlank @Size(max = 200) String password
) {}
