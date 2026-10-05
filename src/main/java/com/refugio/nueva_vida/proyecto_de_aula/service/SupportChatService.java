package com.refugio.nueva_vida.proyecto_de_aula.service;

import com.refugio.nueva_vida.proyecto_de_aula.model.Perro;
import com.refugio.nueva_vida.proyecto_de_aula.repository.PerroRepository;
import com.refugio.nueva_vida.proyecto_de_aula.web.SupportChatRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SupportChatService {

    private static final int MAX_MESSAGE_LENGTH = 1200;
    private static final int MAX_HISTORY_TURNS = 10;
    private static final int MAX_CONTEXT_LENGTH = 8000;
    private static final int MAX_PUBLIC_DATA_LENGTH = 4000;
    private static final int MAX_DESCRIPTION_LENGTH = 280;
    private static final String PUBLIC_APP_CONTEXT = """
        En el sitio se puede crear una cuenta, revisar los perfiles publicados y enviar una solicitud
        de adopción para iniciar el proceso. La preaprobación permite elegir un horario de visita;
        el chat no puede consultar solicitudes, confirmar citas ni realizar cambios en la cuenta.
        No inventes requisitos, horarios ni políticas que no aparezcan en estos datos.
        """;

    private final GeminiChatClient geminiChatClient;
    private final PerroRepository perroRepository;

    public SupportChatService(GeminiChatClient geminiChatClient, PerroRepository perroRepository) {
        this.geminiChatClient = geminiChatClient;
        this.perroRepository = perroRepository;
    }

    public boolean isEnabled() {
        return geminiChatClient.isEnabled();
    }

    public String reply(SupportChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new InvalidChatRequestException("Escribe una pregunta antes de enviar.");
        }
        String message = request.message().trim();
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new InvalidChatRequestException("La pregunta no puede superar 1200 caracteres.");
        }

        List<SupportChatRequest.ChatTurn> history = request.history() == null
            ? List.of() : request.history();
        if (history.size() > MAX_HISTORY_TURNS) {
            throw new InvalidChatRequestException("La conversación es demasiado larga. Inicia un chat nuevo.");
        }

        List<SupportChatRequest.ChatTurn> turns = new ArrayList<>(history.size() + 1);
        int contextLength = message.length();
        String previousRole = null;
        for (SupportChatRequest.ChatTurn turn : history) {
            if (turn == null || turn.text() == null || turn.text().isBlank()
                    || turn.text().length() > MAX_MESSAGE_LENGTH
                    || !("user".equals(turn.role()) || "model".equals(turn.role()))
                    || turn.role().equals(previousRole)) {
                throw new InvalidChatRequestException("El historial de conversación no es válido.");
            }
            contextLength += turn.text().length();
            previousRole = turn.role();
            turns.add(turn);
        }
        if (previousRole != null && !"model".equals(previousRole)) {
            throw new InvalidChatRequestException("El historial de conversación no es válido.");
        }
        if (contextLength > MAX_CONTEXT_LENGTH) {
            throw new InvalidChatRequestException("La conversación es demasiado larga. Inicia un chat nuevo.");
        }
        turns.add(new SupportChatRequest.ChatTurn("user", message));
        return geminiChatClient.generateReply(List.copyOf(turns), buildPublicContext());
    }

    private String buildPublicContext() {
        List<Perro> perros = perroRepository.findByEstadoPublicacion(Perro.EstadoPublicacion.PUBLICADO)
            .stream()
            .sorted(Comparator.comparing(Perro::getNombre, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .toList();
        StringBuilder context = new StringBuilder(PUBLIC_APP_CONTEXT)
            .append("\nPerros publicados y disponibles según el catálogo actual:\n");
        if (perros.isEmpty()) {
            return context.append("No hay perros publicados actualmente.\n").toString();
        }

        int included = 0;
        for (Perro perro : perros) {
            String entry = formatPublicDog(perro);
            if (context.length() + entry.length() > MAX_PUBLIC_DATA_LENGTH) {
                break;
            }
            context.append(entry);
            included++;
        }
        if (included < perros.size()) {
            context.append("Hay ")
                .append(perros.size() - included)
                .append(" perfiles publicados adicionales; indica que consulten el catálogo del sitio.\n");
        }
        return context.toString();
    }

    private String formatPublicDog(Perro perro) {
        String description = cleanPublicText(perro.getDescripcion(), MAX_DESCRIPTION_LENGTH);
        return "- Nombre: " + cleanPublicText(perro.getNombre(), 80)
            + "; edad: " + cleanPublicText(perro.getEdad(), 30)
            + "; sexo: " + (perro.getSexo() == null ? "no informado" : perro.getSexo())
            + "; sociabilidad: " + (perro.getSociabilidad() == null ? "no informada" : perro.getSociabilidad())
            + "; esterilizado: " + publicBoolean(perro.getEsterilizado())
            + "; vacunado: " + publicBoolean(perro.getVacunado())
            + (description.isBlank() ? "" : "; descripción: " + description)
            + "\n";
    }

    private String cleanPublicText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "no informado";
        }
        String cleaned = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ")
            .replaceAll("\\s+", " ")
            .trim();
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength) + "…";
    }

    private String publicBoolean(Boolean value) {
        return value == null ? "no informado" : value ? "sí" : "no";
    }

    public static class InvalidChatRequestException extends RuntimeException {
        public InvalidChatRequestException(String message) {
            super(message);
        }
    }
}
