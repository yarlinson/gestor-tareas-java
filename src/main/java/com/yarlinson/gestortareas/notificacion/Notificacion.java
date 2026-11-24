package com.yarlinson.gestortareas.notificacion;

import com.yarlinson.gestortareas.tarea.Tarea;
import com.yarlinson.gestortareas.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tarea_id", nullable = false, updatable = false)
    private Tarea tarea;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private TipoNotificacion tipo;

    @Column(nullable = false, length = 300, updatable = false)
    private String mensaje;

    @Column(nullable = false)
    private boolean leida;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private Instant creadaEn;

    protected Notificacion() {
        // Requerido por JPA
    }

    public Notificacion(Usuario usuario, Tarea tarea, TipoNotificacion tipo, String mensaje) {
        this.usuario = usuario;
        this.tarea = tarea;
        this.tipo = tipo;
        this.mensaje = mensaje;
    }

    @PrePersist
    void alCrear() {
        this.creadaEn = Instant.now();
    }

    public void marcarLeida() {
        this.leida = true;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Tarea getTarea() {
        return tarea;
    }

    public TipoNotificacion getTipo() {
        return tipo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public boolean isLeida() {
        return leida;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }
}
