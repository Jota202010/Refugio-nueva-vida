package com.refugio.nueva_vida.proyecto_de_aula.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_estado")
public class HistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_perro", nullable = false)
    private Perro perro;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cita", unique = true)
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario adoptante;

    // Estado antes del cambio (null si es el primer registro del animal)
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior")
    private Perro.EstadoPublicacion estadoAnterior;

    // Estado nuevo al que cambió
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false)
    private Perro.EstadoPublicacion estadoNuevo;

    // Fecha y hora exacta del cambio
    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio;

    // Quién o qué originó el cambio
    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false)
    private Origen origen;

    public enum Origen {
        ADMIN,    // el administrador lo cambió manualmente
        SISTEMA   // lo cambió el sistema (ej: al confirmar una cita)
    }

    // ── Constructor de conveniencia ──────────────────────────────────────────
    public HistorialEstado() {}

    public HistorialEstado(Perro perro,
                           Perro.EstadoPublicacion estadoAnterior,
                           Perro.EstadoPublicacion estadoNuevo,
                           Origen origen) {
        this(perro, estadoAnterior, estadoNuevo, origen, null, null);
    }

    public HistorialEstado(Perro perro,
                           Perro.EstadoPublicacion estadoAnterior,
                           Perro.EstadoPublicacion estadoNuevo,
                           Origen origen,
                           Cita cita,
                           Usuario adoptante) {
        this.perro          = perro;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo    = estadoNuevo;
        this.fechaCambio    = LocalDateTime.now();
        this.origen         = origen;
        this.cita           = cita;
        this.adoptante      = adoptante;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public Integer getId()                              { return id; }
    public Perro getPerro()                             { return perro; }
    public Cita getCita()                               { return cita; }
    public Usuario getAdoptante()                       { return adoptante; }
    public Perro.EstadoPublicacion getEstadoAnterior()  { return estadoAnterior; }
    public Perro.EstadoPublicacion getEstadoNuevo()     { return estadoNuevo; }
    public LocalDateTime getFechaCambio()               { return fechaCambio; }
    public Origen getOrigen()                           { return origen; }

    // ── Setters ──────────────────────────────────────────────────────────────
    public void setPerro(Perro perro)                                       { this.perro = perro; }
    public void setCita(Cita cita)                                           { this.cita = cita; }
    public void setAdoptante(Usuario adoptante)                             { this.adoptante = adoptante; }
    public void setEstadoAnterior(Perro.EstadoPublicacion estadoAnterior)   { this.estadoAnterior = estadoAnterior; }
    public void setEstadoNuevo(Perro.EstadoPublicacion estadoNuevo)         { this.estadoNuevo = estadoNuevo; }
    public void setFechaCambio(LocalDateTime fechaCambio)                   { this.fechaCambio = fechaCambio; }
    public void setOrigen(Origen origen)                                    { this.origen = origen; }
}
