package com.yarlinson.gestortareas.tarea;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EtiquetaRepository extends JpaRepository<Etiqueta, Long> {

    List<Etiqueta> findByUsuarioIdAndNombreIn(Long usuarioId, Collection<String> nombres);

    Optional<Etiqueta> findByIdAndUsuarioId(Long id, Long usuarioId);

    @Query("""
            select new com.yarlinson.gestortareas.tarea.EtiquetaResponse(
                e.id,
                e.nombre,
                (select count(t) from Tarea t join t.etiquetas te where te.id = e.id))
            from Etiqueta e
            where e.usuario.id = :usuarioId
            order by e.nombre
            """)
    List<EtiquetaResponse> listarConConteo(@Param("usuarioId") Long usuarioId);
}
