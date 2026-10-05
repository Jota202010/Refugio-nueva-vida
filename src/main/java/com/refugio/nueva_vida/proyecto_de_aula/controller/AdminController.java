package com.refugio.nueva_vida.proyecto_de_aula.controller;

import com.refugio.nueva_vida.proyecto_de_aula.model.Cita;
import com.refugio.nueva_vida.proyecto_de_aula.service.HorarioService;
import com.refugio.nueva_vida.proyecto_de_aula.service.*;
import com.refugio.nueva_vida.proyecto_de_aula.model.Usuario;
import com.refugio.nueva_vida.proyecto_de_aula.web.ConfiguracionHorarioForm;
import com.refugio.nueva_vida.proyecto_de_aula.web.HorarioExcepcionForm;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;

@Controller
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final PerroService perroService;
    private final UsuarioService usuarioService;
    private final CitaService citaService;
    private final HorarioService horarioService;
    private final NotificacionCitaService notificacionCitaService;

    public AdminController(PerroService perroService, UsuarioService usuarioService,
                           CitaService citaService, HorarioService horarioService,
                           NotificacionCitaService notificacionCitaService) {
        this.perroService  = perroService;
        this.usuarioService = usuarioService;
        this.citaService   = citaService;
        this.horarioService = horarioService;
        this.notificacionCitaService = notificacionCitaService;
    }

    // ── Panel principal ───────────────────────────────────────────────────────
    @GetMapping("/admin/panel")
    public String panelAdmin(Model model) {
        model.addAttribute("perros",          perroService.listarTodos());
        model.addAttribute("totalPerros",     perroService.contarTodos());
        model.addAttribute("usuarios",        usuarioService.listarTodos());
        model.addAttribute("totalUsuarios",   usuarioService.contarTodos());
        model.addAttribute("citas",           citaService.listarTodas());
        model.addAttribute("citasPendientes", citaService.contarPendientes());
        return "privilegiado/panel-general-adminview";
    }

    @GetMapping("/admin/horarios")
    public String configuracionHorarios(Model model) {
        model.addAttribute("configuracion", horarioService.cargarFormularioConfiguracion());
        model.addAttribute("excepcion", new HorarioExcepcionForm());
        model.addAttribute("excepciones", horarioService.listarExcepcionesFuturas());
        return "privilegiado/configuracion-horarios-admin";
    }

    @PostMapping("/admin/horarios/semanal")
    public String guardarHorarioSemanal(@ModelAttribute("configuracion") ConfiguracionHorarioForm form,
                                        RedirectAttributes ra) {
        try {
            horarioService.guardarConfiguracion(form);
            ra.addFlashAttribute("mensajeExito",
                "El horario semanal, la duración y la capacidad quedaron actualizados.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/horarios";
    }

    @PostMapping("/admin/horarios/excepcion")
    public String guardarExcepcion(@ModelAttribute("excepcion") HorarioExcepcionForm form,
                                   RedirectAttributes ra) {
        try {
            horarioService.guardarExcepcion(form);
            ra.addFlashAttribute("mensajeExito", "La excepción del día quedó guardada.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/horarios";
    }

    @PostMapping("/admin/horarios/excepcion/{id}/eliminar")
    public String eliminarExcepcion(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            horarioService.eliminarExcepcion(id);
            ra.addFlashAttribute("mensajeExito", "La excepción se eliminó; vuelve a aplicar el horario semanal.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/horarios";
    }

    // ── Perfil del admin ──────────────────────────────────────────────────────
    @GetMapping("/admin/perfil")
    public String perfilAdmin(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Usuario admin = usuarioService.buscarPorUsername(userDetails.getUsername())
            .orElseThrow(() -> new IllegalStateException("Admin no encontrado."));
        var todas       = citaService.listarTodas();
        long aprobadas  = todas.stream().filter(c -> c.getEstado() == Cita.EstadoCita.confirmada).count();
        long rechazadas = todas.stream().filter(c -> c.getEstado() == Cita.EstadoCita.rechazada).count();
        long espera     = todas.stream().filter(c -> c.getEstado() == Cita.EstadoCita.en_espera).count();
        model.addAttribute("admin",            admin);
        model.addAttribute("total_aprobadas",  aprobadas);
        model.addAttribute("total_rechazadas", rechazadas);
        model.addAttribute("total_espera",     espera);
        model.addAttribute("citasAprobadas",   todas.stream().filter(c -> c.getEstado() == Cita.EstadoCita.confirmada).toList());
        model.addAttribute("citasRechazadas",  todas.stream().filter(c -> c.getEstado() == Cita.EstadoCita.rechazada).toList());
        return "privilegiado/mi-perfil-adminview";
    }

    // ── Detalle de usuario ────────────────────────────────────────────────────
    @GetMapping("/admin/usuario/{id}")
    public String detalleUsuario(@PathVariable Integer id, Model model) {
        Usuario u = usuarioService.buscarPorId(id)
            .orElseThrow(() -> new IllegalStateException("Usuario con id " + id + " no encontrado."));
        model.addAttribute("usuario", u);
        model.addAttribute("citas",   citaService.citasDeUsuario(u));
        return "privilegiado/detalle-usuario-adminview";
    }

    // ── Pre-aprobar cita ──────────────────────────────────────────────────────
    @PostMapping("/admin/cita/{id}/pre-aprobar")
    public String preAprobar(@PathVariable Integer id,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes ra) {
        try {
            Usuario admin = usuarioService.buscarPorUsername(userDetails.getUsername()).orElseThrow();
            citaService.preAprobar(id, admin);
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/admin/cita/" + id;
        }
        try {
            notificacionCitaService.enviarAvisoPreaprobacion(id);
            ra.addFlashAttribute("mensajeExito",
                "Solicitud pre-aprobada y correo enviado al usuario para que elija su horario.");
        } catch (MailException e) {
            logger.warn("No se pudo enviar el correo de preaprobación de la cita {}.", id, e);
            ra.addFlashAttribute("avisoCorreo", true);
            ra.addFlashAttribute("mensajeExito",
                "La solicitud quedó pre-aprobada, pero no se pudo enviar el correo. Puedes reintentarlo desde aquí.");
        }
        return "redirect:/admin/cita/" + id;
    }

    @PostMapping("/admin/cita/{id}/reenviar-preaprobacion")
    public String reenviarPreaprobacion(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            notificacionCitaService.enviarAvisoPreaprobacion(id);
            ra.addFlashAttribute("mensajeExito", "Se envió nuevamente el correo para elegir horario.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        } catch (MailException e) {
            logger.warn("No se pudo reenviar el correo de preaprobación de la cita {}.", id, e);
            ra.addFlashAttribute("avisoCorreo", true);
            ra.addFlashAttribute("errorMsg",
                "No se pudo enviar el correo. Verifica la configuración del servidor SMTP e inténtalo de nuevo.");
        }
        return "redirect:/admin/cita/" + id;
    }

    // ── Rechazar cita ─────────────────────────────────────────────────────────
    @PostMapping("/admin/cita/{id}/rechazar")
    public String rechazar(@PathVariable Integer id,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes ra) {
        try {
            Usuario admin = usuarioService.buscarPorUsername(userDetails.getUsername()).orElseThrow();
            citaService.rechazar(id, admin);
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/admin/cita/" + id;
        }
        try {
            notificacionCitaService.enviarAvisoRechazo(id);
            ra.addFlashAttribute("mensajeExito",
                "Solicitud rechazada y se notificó al usuario con un mensaje respetuoso.");
        } catch (MailException e) {
            logger.warn("No se pudo enviar el correo de rechazo de la cita {}.", id, e);
            ra.addFlashAttribute("avisoCorreoRechazo", true);
            ra.addFlashAttribute("mensajeExito",
                "La solicitud quedó rechazada, pero no se pudo enviar el correo. Puedes reintentarlo desde aquí.");
        }
        return "redirect:/admin/cita/" + id;
    }

    @PostMapping("/admin/cita/{id}/reenviar-rechazo")
    public String reenviarRechazo(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            notificacionCitaService.enviarAvisoRechazo(id);
            ra.addFlashAttribute("mensajeExito", "Se envió nuevamente el aviso sobre la solicitud rechazada.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        } catch (MailException e) {
            logger.warn("No se pudo reenviar el correo de rechazo de la cita {}.", id, e);
            ra.addFlashAttribute("avisoCorreoRechazo", true);
            ra.addFlashAttribute("errorMsg",
                "No se pudo enviar el correo. Verifica la configuración del servidor SMTP e inténtalo de nuevo.");
        }
        return "redirect:/admin/cita/" + id;
    }

    @PostMapping("/admin/cita/{id}/confirmar-adopcion")
    public String confirmarAdopcion(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            citaService.confirmarAdopcion(id);
            ra.addFlashAttribute("mensajeExito",
                "Adopción registrada. El historial del perro quedó vinculado con el usuario y esta cita.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/cita/" + id;
    }
}
