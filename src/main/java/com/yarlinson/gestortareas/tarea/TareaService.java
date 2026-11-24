package com.yarlinson.gestortareas.tarea;

import com.yarlinson.gestortareas.common.RecursoNoEncontradoException;
import com.yarlinson.gestortareas.usuario.Usuario;
import com.yarlinson.gestortareas.usuario.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.yarlinson.gestortareas.tarea.TareaSpecs.conEstado;
import static com.yarlinson.gestortareas.tarea.TareaSpecs.contieneTexto;
import static com.yarlinson.gestortareas.tarea.TareaSpecs.deUsuario;

@Service
public class TareaService {

    private final TareaRepository tareas;
    private final UsuarioRepository usuarios;

    public TareaService(TareaRepository tareas, UsuarioRepository usuarios) {
        this.tareas = tareas;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public Page<TareaResponse> listar(Long usuarioId, Boolean completada, String texto, Pageable pageable) {
        Specification<Tarea> filtro = Specification.allOf(
                deUsuario(usuarioId),
                conEstado(completada),
                contieneTexto(texto));
        return tareas.findAll(filtro, pageable).map(TareaResponse::desde);
    }

    @Transactional(readOnly = true)
    public TareaResponse obtener(Long usuarioId, Long id) {
        return TareaResponse.desde(buscarDelUsuario(id, usuarioId));
    }

    @Transactional
    public TareaResponse crear(Long usuarioId, TareaRequest solicitud) {
        Usuario propietario = usuarios.getReferenceById(usuarioId);
        Tarea tarea = new Tarea(
                solicitud.titulo().trim(),
                normalizar(solicitud.descripcion()),
                solicitud.fechaLimite(),
                propietario);
        return TareaResponse.desde(tareas.save(tarea));
    }

    @Transactional
    public TareaResponse actualizar(Long usuarioId, Long id, TareaRequest solicitud) {
        Tarea tarea = buscarDelUsuario(id, usuarioId);
        tarea.actualizar(
                solicitud.titulo().trim(),
                normalizar(solicitud.descripcion()),
                solicitud.fechaLimite());
        return TareaResponse.desde(tarea);
    }

    @Transactional
    public TareaResponse cambiarEstado(Long usuarioId, Long id, boolean completada) {
        Tarea tarea = buscarDelUsuario(id, usuarioId);
        tarea.marcarCompletada(completada);
        return TareaResponse.desde(tarea);
    }

    @Transactional
    public void eliminar(Long usuarioId, Long id) {
        tareas.delete(buscarDelUsuario(id, usuarioId));
    }

    /**
     * Una tarea ajena se trata igual que una inexistente (404), para no revelar
     * qué identificadores pertenecen a otros usuarios.
     */
    private Tarea buscarDelUsuario(Long id, Long usuarioId) {
        return tareas.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarea " + id + " no encontrada"));
    }

    private static String normalizar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
