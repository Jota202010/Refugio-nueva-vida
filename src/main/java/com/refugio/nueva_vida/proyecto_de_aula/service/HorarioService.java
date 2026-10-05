package com.refugio.nueva_vida.proyecto_de_aula.service;

import com.refugio.nueva_vida.proyecto_de_aula.model.*;
import com.refugio.nueva_vida.proyecto_de_aula.repository.*;
import com.refugio.nueva_vida.proyecto_de_aula.web.ConfiguracionHorarioForm;
import com.refugio.nueva_vida.proyecto_de_aula.web.HorarioDiaForm;
import com.refugio.nueva_vida.proyecto_de_aula.web.HorarioExcepcionForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@Transactional
public class HorarioService {

    private static final List<DayOfWeek> DIAS = List.of(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    );
    private static final Map<DayOfWeek, String> NOMBRES_DIAS = Map.of(
        DayOfWeek.MONDAY, "Lunes", DayOfWeek.TUESDAY, "Martes",
        DayOfWeek.WEDNESDAY, "Miércoles", DayOfWeek.THURSDAY, "Jueves",
        DayOfWeek.FRIDAY, "Viernes", DayOfWeek.SATURDAY, "Sábado",
        DayOfWeek.SUNDAY, "Domingo"
    );
    private static final Set<Integer> DURACIONES_PERMITIDAS = Set.of(30, 60, 120, 180);
    private static final int CONFIG_ID = 1;

    private final HorarioRepository horarioRepository;
    private final ConfiguracionHorarioRepository configuracionRepository;
    private final HorarioDiaSemanaRepository diaSemanaRepository;
    private final HorarioExcepcionRepository excepcionRepository;
    private final CitaRepository citaRepository;

    public HorarioService(HorarioRepository horarioRepository,
                          ConfiguracionHorarioRepository configuracionRepository,
                          HorarioDiaSemanaRepository diaSemanaRepository,
                          HorarioExcepcionRepository excepcionRepository,
                          CitaRepository citaRepository) {
        this.horarioRepository = horarioRepository;
        this.configuracionRepository = configuracionRepository;
        this.diaSemanaRepository = diaSemanaRepository;
        this.excepcionRepository = excepcionRepository;
        this.citaRepository = citaRepository;
    }

    public ConfiguracionHorarioForm cargarFormularioConfiguracion() {
        asegurarConfiguracionInicial();
        ConfiguracionHorario configuracion = configuracionRepository.findById(CONFIG_ID).orElseThrow();
        Map<DayOfWeek, HorarioDiaSemana> guardados = new EnumMap<>(DayOfWeek.class);
        diaSemanaRepository.findAll().forEach(d -> guardados.put(d.getDia(), d));

        ConfiguracionHorarioForm form = new ConfiguracionHorarioForm();
        form.setDuracionMinutos(configuracion.getDuracionMinutos());
        form.setProfesionalesDisponibles(configuracion.getProfesionalesDisponibles());
        for (DayOfWeek dia : DIAS) {
            HorarioDiaSemana horario = guardados.get(dia);
            HorarioDiaForm diaForm = new HorarioDiaForm();
            diaForm.setDia(dia);
            diaForm.setNombre(NOMBRES_DIAS.get(dia));
            diaForm.setHabilitado(tieneBloques(horario));
            if (horario != null) {
                diaForm.setMananaInicio(horario.getMananaInicio());
                diaForm.setMananaFin(horario.getMananaFin());
                diaForm.setTardeInicio(horario.getTardeInicio());
                diaForm.setTardeFin(horario.getTardeFin());
            }
            form.getDias().add(diaForm);
        }
        return form;
    }

    public List<HorarioExcepcion> listarExcepcionesFuturas() {
        asegurarConfiguracionInicial();
        return excepcionRepository.findByFechaGreaterThanEqualOrderByFechaAsc(LocalDate.now());
    }

    public void guardarConfiguracion(ConfiguracionHorarioForm form) {
        ConfiguracionHorario configuracion = bloquearConfiguracion();
        validarConfiguracion(form);

        Map<DayOfWeek, HorarioDiaSemana> nuevaSemana = new EnumMap<>(DayOfWeek.class);
        for (HorarioDiaForm diaForm : form.getDias()) {
            HorarioDiaSemana horario = crearHorarioSemana(diaForm);
            nuevaSemana.put(horario.getDia(), horario);
        }
        List<HorarioExcepcion> excepciones = excepcionRepository.findAll();
        validarCitasFuturas(nuevaSemana, excepciones, form.getProfesionalesDisponibles());

        configuracion.setDuracionMinutos(form.getDuracionMinutos());
        configuracion.setProfesionalesDisponibles(form.getProfesionalesDisponibles());
        configuracionRepository.save(configuracion);
        diaSemanaRepository.deleteAllInBatch();
        diaSemanaRepository.saveAll(nuevaSemana.values());
    }

    public void guardarExcepcion(HorarioExcepcionForm form) {
        bloquearConfiguracion();
        if (form.getFecha() == null || form.getFecha().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Selecciona hoy o una fecha futura para la excepción.");
        }
        HorarioExcepcion nueva = new HorarioExcepcion();
        nueva.setFecha(form.getFecha());
        nueva.setCerrado(form.isCerrado());
        if (!form.isCerrado()) {
            validarBloques(form.getMananaInicio(), form.getMananaFin(),
                form.getTardeInicio(), form.getTardeFin());
            nueva.setMananaInicio(form.getMananaInicio());
            nueva.setMananaFin(form.getMananaFin());
            nueva.setTardeInicio(form.getTardeInicio());
            nueva.setTardeFin(form.getTardeFin());
        }

        List<HorarioExcepcion> excepciones = new ArrayList<>(excepcionRepository.findAll());
        excepciones.removeIf(e -> e.getFecha().equals(nueva.getFecha()));
        excepciones.add(nueva);
        Map<DayOfWeek, HorarioDiaSemana> semana = mapaSemana(diaSemanaRepository.findAll());
        int capacidad = configuracionRepository.findById(CONFIG_ID).orElseThrow().getProfesionalesDisponibles();
        validarCitasFuturas(semana, excepciones, capacidad);

        excepcionRepository.findByFecha(nueva.getFecha()).ifPresent(excepcionRepository::delete);
        excepcionRepository.save(nueva);
    }

    public void eliminarExcepcion(Integer id) {
        bloquearConfiguracion();
        HorarioExcepcion actual = excepcionRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("La excepción seleccionada ya no existe."));
        List<HorarioExcepcion> excepciones = new ArrayList<>(excepcionRepository.findAll());
        excepciones.removeIf(e -> e.getId().equals(id));
        Map<DayOfWeek, HorarioDiaSemana> semana = mapaSemana(diaSemanaRepository.findAll());
        int capacidad = configuracionRepository.findById(CONFIG_ID).orElseThrow().getProfesionalesDisponibles();
        validarCitasFuturas(semana, excepciones, capacidad);
        excepcionRepository.delete(actual);
    }

    /**
     * Devuelve turnos dentro de los próximos días configurados, respetando duración,
     * horario semanal, excepciones y capacidad restante.
     */
    public List<HorarioDisponible> generarSlotsDisponibles(int diasHabiles) {
        asegurarConfiguracionInicial();
        ConfiguracionHorario configuracion = configuracionRepository.findById(CONFIG_ID).orElseThrow();
        Map<DayOfWeek, HorarioDiaSemana> semana = mapaSemana(diaSemanaRepository.findAll());
        Map<LocalDate, HorarioExcepcion> excepciones = mapaExcepciones(excepcionRepository.findAll());
        List<HorarioDisponible> slots = new ArrayList<>();
        LocalDate fecha = LocalDate.now();
        int diasAbiertos = 0;
        int objetivoDias = Math.max(0, diasHabiles);
        int limite = Math.max(objetivoDias * 7 + 7, 30);

        for (int intento = 0; diasAbiertos < objetivoDias && intento < limite; intento++, fecha = fecha.plusDays(1)) {
            List<Rango> rangos = rangosParaFecha(fecha, semana, excepciones);
            boolean diaConTurnos = false;
            for (Rango rango : rangos) {
                for (LocalTime hora = rango.inicio(); !hora.plusMinutes(configuracion.getDuracionMinutos()).isAfter(rango.fin());
                     hora = hora.plusMinutes(configuracion.getDuracionMinutos())) {
                    if (fecha.equals(LocalDate.now()) && !hora.isAfter(LocalTime.now())) continue;
                    int ocupados = contarSolapamientos(fecha, hora, configuracion.getDuracionMinutos());
                    if (ocupados >= configuracion.getProfesionalesDisponibles()) continue;
                    HorarioDisponible slot = new HorarioDisponible(fecha, hora);
                    slot.setDuracionMinutos(configuracion.getDuracionMinutos());
                    slots.add(slot);
                    diaConTurnos = true;
                }
            }
            if (diaConTurnos) diasAbiertos++;
        }
        return slots;
    }

    /** Reserva de forma serializada para que no se exceda la capacidad concurrentemente. */
    public HorarioDisponible ocuparPorFechaHora(LocalDate fecha, LocalTime hora, Cita cita) {
        ConfiguracionHorario configuracion = bloquearConfiguracion();
        validarFechaHora(fecha, hora, configuracion);
        validarUsuarioSinCitaSolapada(fecha, hora, configuracion.getDuracionMinutos(), cita);
        if (contarSolapamientos(fecha, hora, configuracion.getDuracionMinutos())
                >= configuracion.getProfesionalesDisponibles()) {
            throw new IllegalStateException(
                "Este horario acaba de completar su capacidad. Por favor elige otro.");
        }
        HorarioDisponible horario = new HorarioDisponible(fecha, hora);
        horario.setOcupado(true);
        horario.setDuracionMinutos(configuracion.getDuracionMinutos());
        horario.setCita(cita);
        return horarioRepository.save(horario);
    }

    private void validarUsuarioSinCitaSolapada(LocalDate fecha, LocalTime hora, int duracion, Cita nuevaCita) {
        if (nuevaCita.getUsuario() == null) return;

        boolean usuarioYaTieneCita = citaRepository.findByUsuario(nuevaCita.getUsuario()).stream()
            .filter(cita -> cita.getEstado() == Cita.EstadoCita.confirmada)
            .filter(cita -> fecha.equals(cita.getFechaCita()) && cita.getHoraCita() != null)
            .anyMatch(cita -> seSolapan(hora, duracion, cita.getHoraCita(), duracionDeCita(cita)));

        if (usuarioYaTieneCita) {
            throw new IllegalStateException(
                "Ya tienes otra cita confirmada que coincide con ese horario. Elige una hora diferente.");
        }
    }

    public int getDuracionConfigurada() {
        asegurarConfiguracionInicial();
        return configuracionRepository.findById(CONFIG_ID).orElseThrow().getDuracionMinutos();
    }

    private void asegurarConfiguracionInicial() {
        if (configuracionRepository.findById(CONFIG_ID).isEmpty()) {
            ConfiguracionHorario nueva = new ConfiguracionHorario();
            nueva.setId(CONFIG_ID);
            nueva.setDuracionMinutos(30);
            nueva.setProfesionalesDisponibles(1);
            configuracionRepository.save(nueva);
        }
        if (diaSemanaRepository.count() == 0) {
            List<HorarioDiaSemana> dias = new ArrayList<>();
            for (DayOfWeek dia : DIAS) {
                HorarioDiaSemana horario = new HorarioDiaSemana();
                horario.setDia(dia);
                if (dia.getValue() <= DayOfWeek.FRIDAY.getValue()) {
                    horario.setMananaInicio(LocalTime.of(8, 0));
                    horario.setMananaFin(LocalTime.of(12, 0));
                    horario.setTardeInicio(LocalTime.of(13, 0));
                    horario.setTardeFin(LocalTime.of(16, 0));
                }
                dias.add(horario);
            }
            diaSemanaRepository.saveAll(dias);
        }
    }

    private ConfiguracionHorario bloquearConfiguracion() {
        asegurarConfiguracionInicial();
        return configuracionRepository.findByIdForUpdate(CONFIG_ID).orElseThrow();
    }

    private void validarConfiguracion(ConfiguracionHorarioForm form) {
        if (form.getDuracionMinutos() == null || !DURACIONES_PERMITIDAS.contains(form.getDuracionMinutos())) {
            throw new IllegalStateException("La duración debe ser 30 minutos, 1, 2 o 3 horas.");
        }
        if (form.getProfesionalesDisponibles() == null
                || form.getProfesionalesDisponibles() < 1 || form.getProfesionalesDisponibles() > 20) {
            throw new IllegalStateException("La capacidad debe estar entre 1 y 20 profesionales.");
        }
        if (form.getDias() == null || form.getDias().size() != DIAS.size()) {
            throw new IllegalStateException("Debes configurar los siete días de la semana.");
        }
        Set<DayOfWeek> recibidos = new HashSet<>();
        for (HorarioDiaForm dia : form.getDias()) {
            if (dia.getDia() == null || !recibidos.add(dia.getDia())) {
                throw new IllegalStateException("La configuración contiene días inválidos o repetidos.");
            }
            if (dia.isHabilitado()) {
                validarBloques(dia.getMananaInicio(), dia.getMananaFin(), dia.getTardeInicio(), dia.getTardeFin());
            }
        }
    }

    private void validarBloques(LocalTime mananaInicio, LocalTime mananaFin,
                                LocalTime tardeInicio, LocalTime tardeFin) {
        validarPar(mananaInicio, mananaFin, "mañana");
        validarPar(tardeInicio, tardeFin, "tarde");
        if (mananaInicio != null && tardeInicio != null && !mananaFin.isBefore(tardeInicio)) {
            throw new IllegalStateException("El bloque de la tarde debe comenzar después del bloque de la mañana.");
        }
        if (mananaInicio == null && tardeInicio == null) {
            throw new IllegalStateException("Ingresa al menos un bloque de atención o marca el día como cerrado.");
        }
    }

    private void validarPar(LocalTime inicio, LocalTime fin, String nombre) {
        if ((inicio == null) != (fin == null)) {
            throw new IllegalStateException("Completa la hora de inicio y fin del bloque de " + nombre + ".");
        }
        if (inicio != null && !inicio.isBefore(fin)) {
            throw new IllegalStateException("La hora de inicio debe ser anterior a la hora de fin (" + nombre + ").");
        }
    }

    private HorarioDiaSemana crearHorarioSemana(HorarioDiaForm form) {
        HorarioDiaSemana horario = new HorarioDiaSemana();
        horario.setDia(form.getDia());
        if (form.isHabilitado()) {
            horario.setMananaInicio(form.getMananaInicio());
            horario.setMananaFin(form.getMananaFin());
            horario.setTardeInicio(form.getTardeInicio());
            horario.setTardeFin(form.getTardeFin());
        }
        return horario;
    }

    private void validarCitasFuturas(Map<DayOfWeek, HorarioDiaSemana> semana,
                                     List<HorarioExcepcion> excepciones, int capacidad) {
        List<Cita> citas = citaRepository.findByEstado(Cita.EstadoCita.confirmada).stream()
            .filter(c -> c.getFechaCita() != null && c.getHoraCita() != null
                && !LocalDateTime.of(c.getFechaCita(), c.getHoraCita()).isBefore(LocalDateTime.now()))
            .toList();
        Map<LocalDate, HorarioExcepcion> mapaExcepciones = mapaExcepciones(excepciones);

        for (Cita cita : citas) {
            int duracion = duracionDeCita(cita);
            boolean enHorario = rangosParaFecha(cita.getFechaCita(), semana,
                    mapaExcepciones).stream()
                .anyMatch(r -> !cita.getHoraCita().isBefore(r.inicio())
                    && !cita.getHoraCita().plusMinutes(duracion).isAfter(r.fin()));
            if (!enHorario) {
                throw new IllegalStateException(
                    "El cambio afecta una cita confirmada del " + cita.getFechaCita()
                    + " a las " + cita.getHoraCita() + ". Reprograma esa cita antes de cambiar el horario.");
            }
        }

        for (Cita cita : citas) {
            LocalTime inicio = cita.getHoraCita();
            int simultaneas = (int) citas.stream()
                .filter(otra -> otra.getFechaCita().equals(cita.getFechaCita()))
                .filter(otra -> seSolapan(inicio, duracionDeCita(cita),
                    otra.getHoraCita(), duracionDeCita(otra)))
                .count();
            if (simultaneas > capacidad) {
                throw new IllegalStateException(
                    "La nueva capacidad es menor que las citas ya confirmadas para el "
                    + cita.getFechaCita() + ". Reprograma las citas antes de reducirla.");
            }
        }
    }

    private void validarFechaHora(LocalDate fecha, LocalTime hora, ConfiguracionHorario configuracion) {
        if (fecha == null || hora == null || fecha.isBefore(LocalDate.now())
                || (fecha.equals(LocalDate.now()) && !hora.isAfter(LocalTime.now()))) {
            throw new IllegalStateException("Selecciona un horario futuro válido.");
        }
        Map<DayOfWeek, HorarioDiaSemana> semana = mapaSemana(diaSemanaRepository.findAll());
        Map<LocalDate, HorarioExcepcion> excepciones = mapaExcepciones(excepcionRepository.findAll());
        boolean dentroDeHorario = rangosParaFecha(fecha, semana, excepciones).stream().anyMatch(r -> {
            if (hora.isBefore(r.inicio()) || hora.plusMinutes(configuracion.getDuracionMinutos()).isAfter(r.fin())) {
                return false;
            }
            long minutosDesdeInicio = java.time.Duration.between(r.inicio(), hora).toMinutes();
            return minutosDesdeInicio % configuracion.getDuracionMinutos() == 0;
        });
        if (!dentroDeHorario) {
            throw new IllegalStateException("El horario elegido ya no está disponible según la configuración actual.");
        }
    }

    private int contarSolapamientos(LocalDate fecha, LocalTime inicio, int duracion) {
        return (int) horarioRepository.findByFechaAndOcupadoTrue(fecha).stream()
            .filter(h -> seSolapan(inicio, duracion, h.getHora(), duracionDeHorario(h)))
            .count();
    }

    private int duracionDeHorario(HorarioDisponible horario) {
        return horario.getDuracionMinutos() == null || horario.getDuracionMinutos() < 1
            ? 30 : horario.getDuracionMinutos();
    }

    private int duracionDeCita(Cita cita) {
        return cita.getHorario() == null ? 30 : duracionDeHorario(cita.getHorario());
    }

    private boolean seSolapan(LocalTime inicioA, int duracionA, LocalTime inicioB, int duracionB) {
        LocalTime finA = inicioA.plusMinutes(duracionA);
        LocalTime finB = inicioB.plusMinutes(duracionB);
        return inicioA.isBefore(finB) && inicioB.isBefore(finA);
    }

    private List<Rango> rangosParaFecha(LocalDate fecha,
                                        Map<DayOfWeek, HorarioDiaSemana> semana,
                                        Map<LocalDate, HorarioExcepcion> excepciones) {
        HorarioExcepcion excepcion = excepciones.get(fecha);
        if (excepcion != null) {
            if (excepcion.isCerrado()) return List.of();
            return rangos(excepcion.getMananaInicio(), excepcion.getMananaFin(),
                excepcion.getTardeInicio(), excepcion.getTardeFin());
        }
        HorarioDiaSemana horario = semana.get(fecha.getDayOfWeek());
        if (horario == null) return List.of();
        return rangos(horario.getMananaInicio(), horario.getMananaFin(),
            horario.getTardeInicio(), horario.getTardeFin());
    }

    private List<Rango> rangos(LocalTime mananaInicio, LocalTime mananaFin,
                               LocalTime tardeInicio, LocalTime tardeFin) {
        List<Rango> rangos = new ArrayList<>(2);
        if (mananaInicio != null && mananaFin != null) rangos.add(new Rango(mananaInicio, mananaFin));
        if (tardeInicio != null && tardeFin != null) rangos.add(new Rango(tardeInicio, tardeFin));
        return rangos;
    }

    private Map<DayOfWeek, HorarioDiaSemana> mapaSemana(List<HorarioDiaSemana> dias) {
        Map<DayOfWeek, HorarioDiaSemana> mapa = new EnumMap<>(DayOfWeek.class);
        dias.forEach(d -> mapa.put(d.getDia(), d));
        return mapa;
    }

    private Map<LocalDate, HorarioExcepcion> mapaExcepciones(List<HorarioExcepcion> excepciones) {
        Map<LocalDate, HorarioExcepcion> mapa = new HashMap<>();
        excepciones.forEach(e -> mapa.put(e.getFecha(), e));
        return mapa;
    }

    private boolean tieneBloques(HorarioDiaSemana horario) {
        return horario != null
            && (horario.getMananaInicio() != null || horario.getTardeInicio() != null);
    }

    private record Rango(LocalTime inicio, LocalTime fin) {}
}
