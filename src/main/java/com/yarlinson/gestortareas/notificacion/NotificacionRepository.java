package com.yarlinson.gestortareas.notificacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    Page<Notificacion> findByUsuarioId(Long usuarioId, Pageable paginacion);

    Page<Notificacion> findByUsuarioIdAndLeida(Long usuarioId, boolean leida, Pageable paginacion);

    Optional<Notificacion> findByIdAndUsuarioId(Long id, Long usuarioId);

    @Modifying
    @Query("delete from Notificacion n where n.tarea.id = :tareaId")
    void eliminarDeLaTarea(@Param("tareaId") Long tareaId);
}
