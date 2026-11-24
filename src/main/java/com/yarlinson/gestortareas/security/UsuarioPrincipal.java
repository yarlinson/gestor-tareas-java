package com.yarlinson.gestortareas.security;

import com.yarlinson.gestortareas.usuario.Rol;
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
    private final Rol rol;

    public UsuarioPrincipal(Usuario usuario) {
        this.id = usuario.getId();
        this.nombreUsuario = usuario.getNombreUsuario();
        this.passwordHash = usuario.getPassword();
        this.rol = usuario.getRol();
    }

    public Long getId() {
        return id;
    }

    /** El rol se lee de la base de datos en cada petición, así que un cambio de rol surte efecto de inmediato. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
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
