package com.yarlinson.gestortareas.tarea;

import com.yarlinson.gestortareas.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tareas")
public class Tarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 1000)
    private String descripcion;

    @Column(nullable = false)
    private boolean completada;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private Instant creadaEn;

    @Column(name = "actualizada_en", nullable = false)
    private Instant actualizadaEn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    protected Tarea() {
        // Requerido por JPA
    }

    public Tarea(String titulo, String descripcion, LocalDate fechaLimite, Usuario usuario) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fechaLimite = fechaLimite;
        this.usuario = usuario;
    }

    @PrePersist
    void alCrear() {
        Instant ahora = Instant.now();
        this.creadaEn = ahora;
        this.actualizadaEn = ahora;
    }

    public void actualizar(String titulo, String descripcion, LocalDate fechaLimite) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fechaLimite = fechaLimite;
        this.actualizadaEn = Instant.now();
    }

    public void marcarCompletada(boolean completada) {
        this.completada = completada;
        this.actualizadaEn = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isCompletada() {
        return completada;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public Instant getActualizadaEn() {
        return actualizadaEn;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
