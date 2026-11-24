package com.yarlinson.gestortareas;

import com.yarlinson.gestortareas.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRETO = "secreto-de-prueba-con-mas-de-32-caracteres-ok";
    private static final String OTRO_SECRETO = "otro-secreto-distinto-con-mas-de-32-caracteres";

    @Test
    @DisplayName("Un token recién emitido se valida y devuelve el usuario")
    void generaYValidaToken() {
        JwtService servicio = new JwtService(SECRETO, 5);

        String token = servicio.generar("ana");

        assertThat(servicio.extraerNombreUsuario(token)).contains("ana");
    }

    @Test
    @DisplayName("Rechaza un token firmado con otra clave")
    void rechazaFirmaDistinta() {
        String token = new JwtService(OTRO_SECRETO, 5).generar("ana");

        assertThat(new JwtService(SECRETO, 5).extraerNombreUsuario(token)).isEmpty();
    }

    @Test
    @DisplayName("Rechaza un token expirado")
    void rechazaTokenExpirado() {
        // Vigencia negativa: el token nace ya vencido.
        JwtService servicio = new JwtService(SECRETO, -1);

        assertThat(servicio.extraerNombreUsuario(servicio.generar("ana"))).isEmpty();
    }

    @Test
    @DisplayName("Rechaza texto que no es un JWT")
    void rechazaTokenMalFormado() {
        JwtService servicio = new JwtService(SECRETO, 5);

        assertThat(servicio.extraerNombreUsuario("esto.no.es.un.jwt")).isEmpty();
        assertThat(servicio.extraerNombreUsuario("")).isEmpty();
    }

    @Test
    @DisplayName("Exige un secreto de al menos 32 caracteres")
    void exigeSecretoLargo() {
        assertThatThrownBy(() -> new JwtService("corto", 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }
}
