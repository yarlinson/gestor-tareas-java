package com.yarlinson.gestortareas.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Registro e inicio de sesión")
public class AuthController {

    private final AuthService servicio;

    public AuthController(AuthService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un usuario y devuelve su token JWT")
    public TokenResponse registrar(@Valid @RequestBody RegistroRequest solicitud) {
        return servicio.registrar(solicitud);
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión y devuelve un token JWT")
    public TokenResponse iniciarSesion(@Valid @RequestBody LoginRequest solicitud) {
        return servicio.iniciarSesion(solicitud);
    }
}
