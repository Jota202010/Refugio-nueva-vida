package com.refugio.nueva_vida.proyecto_de_aula.service;

import com.refugio.nueva_vida.proyecto_de_aula.model.Cita;
import com.refugio.nueva_vida.proyecto_de_aula.model.Cita.EstadoEvaluacionIa;
import com.refugio.nueva_vida.proyecto_de_aula.model.Perro;
import com.refugio.nueva_vida.proyecto_de_aula.service.GeminiChatClient.ChatProviderException;
import com.refugio.nueva_vida.proyecto_de_aula.service.GeminiChatClient.ChatUnavailableException;
import com.refugio.nueva_vida.proyecto_de_aula.web.EvaluacionAdopcionRespuesta;
import com.refugio.nueva_vida.proyecto_de_aula.web.EvaluacionAdopcionSolicitud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EvaluacionAutomaticaAdopcionService {

    private static final Logger logger =
        LoggerFactory.getLogger(EvaluacionAutomaticaAdopcionService.class);

    private final GeminiChatClient geminiChatClient;

    public EvaluacionAutomaticaAdopcionService(GeminiChatClient geminiChatClient) {
        this.geminiChatClient = geminiChatClient;
    }

    public Resultado evaluar(Cita cita) {
        if (!Boolean.TRUE.equals(cita.getAceptaEvaluacionIa())) {
            return revision(EstadoEvaluacionIa.sin_consentimiento,
                "El solicitante no acepto el analisis automatizado; requiere revision humana.");
        }

        String motivoRevision = motivoRevisionPorCriterios(cita);
        if (motivoRevision != null) {
            return revision(EstadoEvaluacionIa.revision_humana, motivoRevision);
        }

        try {
            EvaluacionAdopcionRespuesta respuesta =
                geminiChatClient.evaluarSolicitudAdopcion(crearSolicitud(cita));
            if (EvaluacionAdopcionRespuesta.Decision.PRE_APPROVE.name()
                    .equals(respuesta.decision())) {
                return revision(EstadoEvaluacionIa.preaprobada, respuesta.summary());
            }
            if (EvaluacionAdopcionRespuesta.Decision.HUMAN_REVIEW.name()
                    .equals(respuesta.decision())) {
                return revision(EstadoEvaluacionIa.revision_humana, respuesta.summary());
            }
            logger.warn("Gemini devolvio una decision desconocida para la solicitud {}.", cita.getIdCita());
            return revision(EstadoEvaluacionIa.error,
                "La evaluacion automatica devolvio un resultado no valido; requiere revision humana.");
        } catch (ChatUnavailableException | ChatProviderException exception) {
            logger.warn("La evaluacion automatica de la solicitud {} no pudo completarse.",
                cita.getIdCita(), exception);
            return revision(EstadoEvaluacionIa.error,
                "La evaluacion automatica no estuvo disponible; requiere revision humana.");
        }
    }

    private String motivoRevisionPorCriterios(Cita cita) {
        if (cita.getTipoVivienda() == null || cita.getPropiedad() == null
                || cita.getTodosAcuerdo() == null || cita.getMascotasActual() == null
                || cita.getHorasSolo() == null || cita.getPuedePasear() == null
                || cita.getCubreVet() == null || cita.getCubreEmergencias() == null
                || cita.getCondNoAbandono() == null || cita.getCondSeguimiento() == null
                || cita.getCondEvaluacion() == null) {
            return "Faltan respuestas necesarias para la preaprobacion automatica.";
        }
        if (cita.getPropiedad() == Cita.Propiedad.alquilada
                && cita.getPermitenMascotas() != Cita.PermitenMascotas.si) {
            return "Se debe confirmar con el solicitante o el refugio el permiso para tener mascotas.";
        }
        if (!Boolean.TRUE.equals(cita.getTodosAcuerdo())) {
            return "El acuerdo de todas las personas del hogar requiere revision.";
        }
        if (!Boolean.FALSE.equals(cita.getMascotasActual())) {
            return "La convivencia con mascotas actuales requiere revision humana.";
        }
        if (!"Menos de 2 horas".equals(cita.getHorasSolo())
                && !"2-4 horas".equals(cita.getHorasSolo())) {
            return "El tiempo que el perro estaria solo requiere revision humana.";
        }
        if (!Boolean.TRUE.equals(cita.getPuedePasear())
                || !Boolean.TRUE.equals(cita.getCubreVet())
                || !Boolean.TRUE.equals(cita.getCubreEmergencias())
                || !Boolean.TRUE.equals(cita.getCondNoAbandono())
                || !Boolean.TRUE.equals(cita.getCondSeguimiento())
                || !Boolean.TRUE.equals(cita.getCondEvaluacion())) {
            return "Una o mas condiciones de cuidado requieren revision humana.";
        }

        Perro perro = cita.getPerro();
        if (perro == null || perro.getEdad() == null || perro.getSexo() == null
                || perro.getSociabilidad() == null || perro.getDescripcion() == null
                || perro.getDescripcion().isBlank()) {
            return "El perfil publico del perro no contiene suficiente informacion para evaluar compatibilidad.";
        }
        return null;
    }

    private EvaluacionAdopcionSolicitud crearSolicitud(Cita cita) {
        Perro perro = cita.getPerro();
        return new EvaluacionAdopcionSolicitud(
            new EvaluacionAdopcionSolicitud.Aplicacion(
                cita.getTipoVivienda().name(),
                cita.getPropiedad().name(),
                cita.getPropiedad() == Cita.Propiedad.alquilada
                    ? cita.getPermitenMascotas().name()
                    : Cita.PermitenMascotas.no_aplica.name(),
                cita.getTodosAcuerdo(),
                cita.getMascotasActual(),
                cita.getHorasSolo(),
                cita.getPuedePasear(),
                cita.getCubreVet(),
                cita.getCubreEmergencias(),
                cita.getCondNoAbandono(),
                cita.getCondSeguimiento(),
                cita.getCondEvaluacion()
            ),
            new EvaluacionAdopcionSolicitud.PerroPublico(
                perro.getEdad(),
                perro.getSexo().name(),
                perro.getSociabilidad().name(),
                limpiarDescripcion(perro.getDescripcion())
            )
        );
    }

    private String limpiarDescripcion(String descripcion) {
        String cleaned = descripcion.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ")
            .replaceAll("\\s+", " ")
            .trim();
        return cleaned.substring(0, Math.min(cleaned.length(), 280));
    }

    private Resultado revision(EstadoEvaluacionIa estado, String motivo) {
        return new Resultado(estado, motivo);
    }

    public record Resultado(EstadoEvaluacionIa estado, String motivo) {}
}
