package com.yarlinson.gestortareas.notificacion;

import com.yarlinson.gestortareas.tarea.Tarea;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/** Busca las tareas que todavía no tienen su aviso; el límite del lote lo da {@code Pageable}. */
public interface RecordatorioRepository extends Repository<Tarea, Long> {

    @Query("""
            select t from Tarea t
            where t.completada = false
              and t.fechaLimite >= :desde and t.fechaLimite <= :hasta
              and not exists (select n.id from Notificacion n where n.tarea = t and n.tipo = :tipo)
            order by t.id
            """)
    List<Tarea> sinAvisoDeVencimientoProximo(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("tipo") TipoNotificacion tipo,
            Pageable lote);

    @Query("""
            select t from Tarea t
            where t.completada = false
              and t.fechaLimite < :hoy
              and not exists (select n.id from Notificacion n where n.tarea = t and n.tipo = :tipo)
            order by t.id
            """)
    List<Tarea> sinAvisoDeVencida(
            @Param("hoy") LocalDate hoy,
            @Param("tipo") TipoNotificacion tipo,
            Pageable lote);
}
