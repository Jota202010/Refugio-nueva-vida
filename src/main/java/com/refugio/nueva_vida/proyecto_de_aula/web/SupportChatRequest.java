package com.refugio.nueva_vida.proyecto_de_aula.web;

import java.util.List;

public record SupportChatRequest(String message, List<ChatTurn> history) {
    public record ChatTurn(String role, String text) {}
}
