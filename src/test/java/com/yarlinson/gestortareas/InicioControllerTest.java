package com.yarlinson.gestortareas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InicioControllerTest extends IntegracionBase {

    @Test
    @DisplayName("La URL raíz redirige a Swagger UI sin necesitar token")
    void raizRedirigeAlaDocumentacion() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/swagger-ui.html"));
    }

    @Test
    @DisplayName("La especificación OpenAPI y el estado del servicio son públicos")
    void documentacionYEstadoSonPublicos() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
