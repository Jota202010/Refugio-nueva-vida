package com.refugio.nueva_vida.proyecto_de_aula.web;

import java.util.List;

public record JwtPrincipalResponse(String username, List<String> roles) {}
