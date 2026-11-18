package com.refugio.nueva_vida.proyecto_de_aula.controller;

import com.refugio.nueva_vida.proyecto_de_aula.model.Cita;
import com.refugio.nueva_vida.proyecto_de_aula.model.Perro;
import com.refugio.nueva_vida.proyecto_de_aula.model.Usuario;
import com.refugio.nueva_vida.proyecto_de_aula.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.mail.MailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

@Controller
public class CitaController {

    private static final Logger logger = LoggerFactory.getLogger(CitaController.class);

    private final CitaService    citaService;
    private final PerroService   perroService;
    private final UsuarioService usuarioService;
    private final HorarioService horarioService;
    private final EvaluacionAutomaticaAdopcionService evaluacionAutomaticaService;
    private final NotificacionCitaService notificacionCitaService;

    public CitaController(CitaService citaService, PerroService perroService,
                          UsuarioService usuarioService, HorarioService horarioService,
                          EvaluacionAutomaticaAdopcionService evaluacionAutomaticaService,
                          NotificacionCitaService notificacionCitaService) {
        this.citaService    = citaService;
        this.perroService   = perroService;
        this.usuarioService = usuarioService;
        this.horarioService = horarioService;
        this.evaluacionAutomaticaService = evaluacionAutomaticaService;
        this.notificacionCitaService = notificacionCitaService;
    }

    // ── Formulario solicitud adopción (GET) ───────────────────────────────────
    @GetMapping("/agendar-cita/{perroId}")
    public String agendarForm(@PathVariable Integer perroId,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes ra,
                              Model model) {
        // Verificar que el animal existe
        Perro perro = perroService.buscarPorId(perroId).orElse(null);
        if (perro == null) {
            ra.addFlashAttribute("errorMsg", "El animal que buscas no existe.");
            return "redirect:/inicio";
        }

        // Verificar que el animal está disponible para adopción
        if (perro.getEstadoPublicacion() != Perro.EstadoPublicacion.PUBLICADO) {
            ra.addFlashAttribute("errorMsg",
                "El animal '" + perro.getNombre() + "' ya no está disponible para adopción en este momento.");
            return "redirect:/inicio";
        }

        // Verificar si el usuario ya tiene una solicitud activa con este animal
        if (userDetails != null) {
            Usuario usuario = usuarioService.buscarPorUsername(userDetails.getUsername()).orElse(null);
            if (usuario != null) {
                boolean yaExiste = citaService.citasDeUsuario(usuario).stream()
                    .anyMatch(c -> c.getPerro().getIdPerro().equals(perroId)
                               && c.getEstado() != Cita.EstadoCita.rechazada);
                if (yaExiste) {
                    ra.addFlashAttribute("avisoCita",
                        "¡Aviso! Ya tienes una solicitud activa para " + perro.getNombre() +
                        ". Revisa tu perfil para ver el estado actual.");
                    return "redirect:/mascota/" + perroId;
                }
            }
        }

        model.addAttribute("perro", perro);
        model.addAttribute("cita",  new Cita());
        model.addAttribute("tiposVivienda",    Cita.TipoVivienda.values());
        model.addAttribute("propiedades",      Cita.Propiedad.values());
        model.addAttribute("opcionesMascotas", Cita.PermitenMascotas.values());
        return "usuario/agendar-cita";
    }

    // ── Enviar solicitud adopción (POST) ──────────────────────────────────────
    @PostMapping("/agendar-cita/{perroId}")
    public String enviarSolicitud(@PathVariable Integer perroId,
                                  @ModelAttribute Cita cita,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes ra) {
        Perro perro = perroService.buscarPorId(perroId).orElse(null);
        if (perro == null) {
            ra.addFlashAttribute("errorMsg", "El animal seleccionado no existe.");
            return "redirect:/inicio";
        }
        Usuario usuario = usuarioService.buscarPorUsername(userDetails.getUsername()).orElse(null);
        if (usuario == null) {
            ra.addFlashAttribute("errorMsg", "Sesión expirada. Inicia sesión de nuevo.");
            return "redirect:/login";
        }

        cita.setPerro(perro);
        cita.setUsuario(usuario);
        cita.setEstado(Cita.EstadoCita.en_espera);
        cita.setFechaCita(null);
        cita.setHoraCita(null);
        cita.setFechaDecision(null);
        cita.setAdmin(null);
        cita.setEvaluacionIaEstado(null);
        cita.setEvaluacionIaMotivo(null);
        cita.setEvaluacionIaFecha(null);
        cita.setAceptaEvaluacionIa(Boolean.TRUE.equals(cita.getAceptaEvaluacionIa()));

        try {
            Cita guardada = citaService.guardar(cita);
            EvaluacionAutomaticaAdopcionService.Resultado evaluacion =
                evaluacionAutomaticaService.evaluar(guardada);
            Cita procesada = citaService.registrarEvaluacionAutomatica(
                guardada.getIdCita(), evaluacion.estado(), evaluacion.motivo());

            if (procesada.getEstado() == Cita.EstadoCita.pre_aprobada) {
                try {
                    notificacionCitaService.enviarAvisoPreaprobacion(procesada.getIdCita());
                    ra.addFlashAttribute("mensajeExito",
                        "Tu solicitud fue pre-aprobada automaticamente. Revisa tu correo para elegir un horario. "
                            + "La adopcion aun no esta confirmada.");
                } catch (MailException exception) {
                    logger.warn("No se pudo enviar el correo de preaprobacion automatica de la cita {}.",
                        procesada.getIdCita(), exception);
                    ra.addFlashAttribute("mensajeExito",
                        "Tu solicitud fue pre-aprobada automaticamente, pero no se pudo enviar el correo. "
                            + "El equipo del refugio podra ayudarte con el siguiente paso.");
                }
            } else {
                ra.addFlashAttribute("mensajeExito",
                    "Tu solicitud fue recibida y quedo pendiente de revision por el equipo del refugio.");
            }
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/perfil";
    }

    // ── Elegir horario (GET) — solo si cita está pre_aprobada ─────────────────
    @GetMapping("/cita/{citaId}/elegir-horario")
    public String elegirHorarioForm(@PathVariable Integer citaId,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    RedirectAttributes ra,
                                    Model model) {
        Cita cita = citaService.buscarPorId(citaId).orElse(null);
        if (cita == null) {
            ra.addFlashAttribute("errorMsg", "La cita no existe.");
            return "redirect:/perfil";
        }
        Usuario usuario = usuarioService.buscarPorUsername(userDetails.getUsername()).orElse(null);
        if (usuario == null) {
            return "redirect:/login";
        }
        // Validar que la cita pertenece al usuario
        if (!cita.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
            ra.addFlashAttribute("errorMsg", "No tienes permiso para acceder a esta cita.");
            return "redirect:/perfil";
        }
        // Validar que la cita está en estado pre-aprobada
        if (cita.getEstado() != Cita.EstadoCita.pre_aprobada) {
            ra.addFlashAttribute("errorMsg",
                "Esta cita no está en estado pre-aprobada. No es posible elegir horario ahora.");
            return "redirect:/perfil";
        }

        model.addAttribute("cita", cita);
        model.addAttribute("horariosDisponibles", horarioService.generarSlotsDisponibles(14));
        int duracion = horarioService.getDuracionConfigurada();
        model.addAttribute("duracionTexto", duracion < 60
            ? duracion + " minutos"
            : (duracion / 60) + (duracion == 60 ? " hora" : " horas"));
        return "usuario/elegir-horario";
    }

    // ── Confirmar horario elegido (POST) ──────────────────────────────────────
    @PostMapping("/cita/{citaId}/confirmar-horario")
    public String confirmarHorario(@PathVariable Integer citaId,
                                   @RequestParam(required = false) String fecha,
                                   @RequestParam(required = false) String hora,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes ra) {
        Cita cita = citaService.buscarPorId(citaId).orElse(null);
        if (cita == null) {
            ra.addFlashAttribute("errorMsg", "La cita no existe.");
            return "redirect:/perfil";
        }
        Usuario usuario = usuarioService.buscarPorUsername(userDetails.getUsername()).orElse(null);
        if (usuario == null) return "redirect:/login";

        if (!cita.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
            ra.addFlashAttribute("errorMsg", "No tienes permiso para confirmar esta cita.");
            return "redirect:/perfil";
        }

        // Validar que fecha y hora no estén vacías
        if (fecha == null || fecha.isBlank() || hora == null || hora.isBlank()) {
            ra.addFlashAttribute("errorMsg", "Debes seleccionar un día y una hora antes de confirmar.");
            return "redirect:/cita/" + citaId + "/elegir-horario";
        }

        try {
            LocalDate fechaParsed = LocalDate.parse(fecha.trim());
            LocalTime horaParsed  = LocalTime.parse(hora.trim());
            citaService.confirmar(citaId, fechaParsed, horaParsed);
            ra.addFlashAttribute("mensajeExito", "¡Cita confirmada! Ya tienes tu fecha y hora reservada.");
        } catch (DateTimeParseException e) {
            ra.addFlashAttribute("errorMsg",
                "El formato de fecha u hora no es válido. Por favor selecciona el horario desde el calendario.");
            return "redirect:/cita/" + citaId + "/elegir-horario";
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/cita/" + citaId + "/elegir-horario";
        }
        return "redirect:/perfil";
    }

    // ── Detalle de cita (admin) ───────────────────────────────────────────────
    @GetMapping("/admin/cita/{id}")
    public String detalleCita(@PathVariable Integer id, RedirectAttributes ra, Model model) {
        Cita cita = citaService.buscarPorId(id).orElse(null);
        if (cita == null) {
            ra.addFlashAttribute("errorMsg", "La cita con id " + id + " no existe.");
            return "redirect:/admin/panel";
        }
        model.addAttribute("cita", cita);
        return "privilegiado/detalle-cita-admin";
    }
}
