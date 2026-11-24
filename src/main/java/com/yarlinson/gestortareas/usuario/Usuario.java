package com.yarlinson.gestortareas.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_usuario", nullable = false, unique = true, length = 50)
    private String nombreUsuario;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    /** Hash BCrypt de la contraseña; nunca se guarda en texto plano. */
    @Column(nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Rol rol = Rol.USER;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    protected Usuario() {
        // Requerido por JPA
    }

    /** Crea un usuario corriente (rol {@code USER}). */
    public Usuario(String nombreUsuario, String email, String passwordHash) {
        this(nombreUsuario, email, passwordHash, Rol.USER);
    }

    public Usuario(String nombreUsuario, String email, String passwordHash, Rol rol) {
        this.nombreUsuario = nombreUsuario;
        this.email = email;
        this.password = passwordHash;
        this.rol = rol;
    }

    @PrePersist
    void alCrear() {
        this.creadoEn = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Rol getRol() {
        return rol;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
