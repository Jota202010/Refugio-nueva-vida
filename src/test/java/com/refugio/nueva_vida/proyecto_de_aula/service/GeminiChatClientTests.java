package com.refugio.nueva_vida.proyecto_de_aula.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.refugio.nueva_vida.proyecto_de_aula.web.SupportChatRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiChatClientTests {

    @Test
    void llamaElProxyHttpsSinEnviarUnaClaveDeGeminiAlContenedorLocal() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(
                "https://proxy.example.test/v1beta/models/gemini-3.5-flash-lite:generateContent"))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andExpect(headerDoesNotExist("x-goog-api-key"))
            .andExpect(jsonPath("$.systemInstruction.parts[0].text",
                org.hamcrest.Matchers.containsString("Eres el asistente virtual de Refugio Nueva Vida")))
            .andExpect(jsonPath("$.context").value("Datos públicos actuales: Luna está publicada."))
            .andExpect(jsonPath("$.systemInstruction.parts[0].text",
                org.hamcrest.Matchers.containsString("Luna está publicada.")))
            .andExpect(jsonPath("$.contents[0].role").value("user"))
            .andExpect(jsonPath("$.contents[0].parts[0].text").value("Hola"))
            .andExpect(jsonPath("$.generationConfig.maxOutputTokens").value(500))
            .andRespond(withSuccess("""
                {"candidates":[{"content":{"parts":[{"text":"¡Hola! ¿En qué te ayudo?"}]}}]}
                """, org.springframework.http.MediaType.APPLICATION_JSON));

        GeminiChatClient client = new GeminiChatClient(
            builder, new ObjectMapper(), "", "gemini-3.5-flash-lite", "https://proxy.example.test/", false);

        assertTrue(client.isEnabled());
        assertEquals("¡Hola! ¿En qué te ayudo?", client.generateReply(List.of(
            new SupportChatRequest.ChatTurn("user", "Hola")
        ), "Datos públicos actuales: Luna está publicada."));
        server.verify();
    }

    @Test
    void rechazaProxyQueNoUsaHttps() {
        assertThrows(IllegalArgumentException.class, () -> new GeminiChatClient(
            RestClient.builder(), new ObjectMapper(), "", "gemini-3.5-flash-lite", "http://proxy.example.test"));
    }
}
