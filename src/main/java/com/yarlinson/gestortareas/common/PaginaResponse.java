package com.yarlinson.gestortareas.common;

import org.springframework.data.domain.Page;

import java.util.List;

/** Respuesta paginada con una forma estable, independiente de la serialización interna de Spring Data. */
public record PaginaResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas) {

    public static <T> PaginaResponse<T> desde(Page<T> pagina) {
        return new PaginaResponse<>(
                pagina.getContent(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
