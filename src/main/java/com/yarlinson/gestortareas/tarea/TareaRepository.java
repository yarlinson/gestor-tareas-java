package com.yarlinson.gestortareas.tarea;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface TareaRepository extends JpaRepository<Tarea, Long>, JpaSpecificationExecutor<Tarea> {

    /** Busca una tarea solo si pertenece al usuario indicado (aislamiento por usuario). */
    Optional<Tarea> findByIdAndUsuarioId(Long id, Long usuarioId);
}
