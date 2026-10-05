package com.refugio.nueva_vida.proyecto_de_aula.controller;

import com.refugio.nueva_vida.proyecto_de_aula.service.GeminiChatClient.ChatProviderException;
import com.refugio.nueva_vida.proyecto_de_aula.service.GeminiChatClient.ChatUnavailableException;
import com.refugio.nueva_vida.proyecto_de_aula.service.SupportChatService;
import com.refugio.nueva_vida.proyecto_de_aula.service.SupportChatService.InvalidChatRequestException;
import com.refugio.nueva_vida.proyecto_de_aula.web.SupportChatRequest;
import com.refugio.nueva_vida.proyecto_de_aula.web.SupportChatResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayDeque;
import java.util.Map;

@RestController
@RequestMapping("/api/support-chat")
public class SupportChatController {

    private static final String REQUEST_TIMES_SESSION_KEY = "supportChatRequestTimes";
    private static final long RATE_WINDOW_MILLIS = 10 * 60 * 1000L;
    private static final int MAX_REQUESTS_PER_WINDOW = 12;

    private final SupportChatService supportChatService;

    public SupportChatController(SupportChatService supportChatService) {
        this.supportChatService = supportChatService;
    }

    @GetMapping("/status")
    public Map<String, Boolean> status() {
        return Map.of("enabled", supportChatService.isEnabled());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> malformedRequest() {
        return error(HttpStatus.BAD_REQUEST, "La pregunta no tiene un formato válido.");
    }

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody(required = false) SupportChatRequest request,
                                  HttpSession session) {
        if (!supportChatService.isEnabled()) {
            return error(HttpStatus.SERVICE_UNAVAILABLE,
                "El asistente aún no está configurado. El refugio puede atenderte por sus canales de contacto.");
        }
        if (!registrarSolicitud(session)) {
            return error(HttpStatus.TOO_MANY_REQUESTS,
                "Has enviado varias preguntas seguidas. Espera un momento antes de volver a intentarlo.");
        }

        try {
            return ResponseEntity.ok(new SupportChatResponse(supportChatService.reply(request)));
        } catch (InvalidChatRequestException exception) {
            return error(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (ChatUnavailableException exception) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        } catch (ChatProviderException exception) {
            return error(HttpStatus.BAD_GATEWAY,
                "El asistente no pudo responder en este momento. Inténtalo de nuevo más tarde.");
        }
    }

    private boolean registrarSolicitud(HttpSession session) {
        synchronized (session) {
            @SuppressWarnings("unchecked")
            ArrayDeque<Long> solicitudes = (ArrayDeque<Long>) session.getAttribute(REQUEST_TIMES_SESSION_KEY);
            if (solicitudes == null) {
                solicitudes = new ArrayDeque<>();
                session.setAttribute(REQUEST_TIMES_SESSION_KEY, solicitudes);
            }

            long ahora = System.currentTimeMillis();
            while (!solicitudes.isEmpty() && ahora - solicitudes.peekFirst() >= RATE_WINDOW_MILLIS) {
                solicitudes.removeFirst();
            }
            if (solicitudes.size() >= MAX_REQUESTS_PER_WINDOW) {
                return false;
            }
            solicitudes.addLast(ahora);
            return true;
        }
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
