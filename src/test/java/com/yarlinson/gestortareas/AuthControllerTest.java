package com.yarlinson.gestortareas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends IntegracionBase {

    private String cuerpoRegistro(String nombre, String email, String password) throws Exception {
        return json.writeValueAsString(Map.of(
                "nombreUsuario", nombre,
                "email", email,
                "password", password));
    }

    private String cuerpoLogin(String nombre, String password) throws Exception {
        return json.writeValueAsString(Map.of("nombreUsuario", nombre, "password", password));
    }

    @Test
    @DisplayName("Registrar un usuario devuelve 201 y un token Bearer")
    void registroExitoso() throws Exception {
        String nombre = nombreUnico();

        mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro(nombre, nombre + "@correo.test", CLAVE_VALIDA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEnSegundos").value(300));
    }

    @Test
    @DisplayName("No permite repetir nombre de usuario ni correo (409)")
    void registroDuplicado() throws Exception {
        String nombre = nombreUnico();
        String email = nombre + "@correo.test";
        registrarYObtenerToken(nombre);

        mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro(nombre, "otro-" + email, CLAVE_VALIDA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El nombre de usuario ya está en uso"));

        mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro(nombreUnico(), email.toUpperCase(), CLAVE_VALIDA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El correo ya está registrado"));
    }

    @Test
    @DisplayName("Valida los datos del registro (400) e informa el detalle por campo")
    void registroInvalido() throws Exception {
        mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro("ab", "no-es-un-correo", "corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombreUsuario").exists())
                .andExpect(jsonPath("$.errores.email").exists())
                .andExpect(jsonPath("$.errores.password").exists());
    }

    @Test
    @DisplayName("Iniciar sesión con credenciales correctas devuelve un token")
    void loginExitoso() throws Exception {
        String nombre = nombreUnico();
        registrarYObtenerToken(nombre);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(nombre, CLAVE_VALIDA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("Contraseña incorrecta o usuario inexistente devuelven 401")
    void loginConCredencialesIncorrectas() throws Exception {
        String nombre = nombreUnico();
        registrarYObtenerToken(nombre);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(nombre, "OtraClaveDistinta1")))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(nombreUnico(), CLAVE_VALIDA)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un token manipulado no da acceso a rutas protegidas")
    void tokenManipuladoEsRechazado() throws Exception {
        String token = registrarYObtenerToken(nombreUnico());

        mvc.perform(get("/api/tareas").header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/tareas").header("Authorization", bearer(alterarFirma(token))))
                .andExpect(status().isUnauthorized());
    }

    /** Cambia un carácter del centro de la firma (todos sus bits son significativos). */
    private static String alterarFirma(String token) {
        int posicion = token.lastIndexOf('.') + 10;
        char original = token.charAt(posicion);
        char distinto = original == 'A' ? 'B' : 'A';
        return token.substring(0, posicion) + distinto + token.substring(posicion + 1);
    }
}
