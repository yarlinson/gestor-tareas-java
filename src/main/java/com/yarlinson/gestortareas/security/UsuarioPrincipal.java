package com.yarlinson.gestortareas.security;

import com.yarlinson.gestortareas.usuario.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Usuario autenticado; conserva el id para que los controladores filtren por propietario. */
public class UsuarioPrincipal implements UserDetails {

    private final Long id;
    private final String nombreUsuario;
    private final String passwordHash;

    public UsuarioPrincipal(Usuario usuario) {
        this.id = usuario.getId();
        this.nombreUsuario = usuario.getNombreUsuario();
        this.passwordHash = usuario.getPassword();
    }

    public Long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return nombreUsuario;
    }
}
