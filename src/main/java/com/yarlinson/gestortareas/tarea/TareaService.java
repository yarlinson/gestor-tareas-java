package com.yarlinson.gestortareas.tarea;

import com.yarlinson.gestortareas.common.RecursoNoEncontradoException;
import com.yarlinson.gestortareas.usuario.Usuario;
import com.yarlinson.gestortareas.usuario.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

import static com.yarlinson.gestortareas.tarea.TareaSpecs.conEstado;
import static com.yarlinson.gestortareas.tarea.TareaSpecs.conEtiqueta;
import static com.yarlinson.gestortareas.tarea.TareaSpecs.conPrioridad;
import static com.yarlinson.gestortareas.tarea.TareaSpecs.contieneTexto;
import static com.yarlinson.gestortareas.tarea.TareaSpecs.deUsuario;
import static com.yarlinson.gestortareas.tarea.TareaSpecs.vencidas;

@Service
public class TareaService {

    private final TareaRepository tareas;
    private final UsuarioRepository usuarios;
    private final EtiquetaService etiquetas;
    private final Clock reloj;

    public TareaService(
            TareaRepository tareas,
            UsuarioRepository usuarios,
            EtiquetaService etiquetas,
            Clock reloj) {
        this.tareas = tareas;
        this.usuarios = usuarios;
        this.etiquetas = etiquetas;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Page<TareaResponse> listar(Long usuarioId, FiltroTareas filtro, Pageable pageable) {
        LocalDate hoy = hoy();
        Specification<Tarea> criterios = Specification.allOf(
                deUsuario(usuarioId),
                conEstado(filtro.completada()),
                contieneTexto(filtro.texto()),
                conPrioridad(filtro.prioridad()),
                conEtiqueta(filtro.etiqueta()),
                vencidas(filtro.vencidas(), hoy));
        return tareas.findAll(criterios, pageable).map(tarea -> TareaResponse.desde(tarea, hoy));
    }

    @Transactional(readOnly = true)
    public TareaResponse obtener(Long usuarioId, Long id) {
        return TareaResponse.desde(buscarDelUsuario(id, usuarioId), hoy());
    }

    @Transactional
    public TareaResponse crear(Long usuarioId, TareaRequest solicitud) {
        Usuario propietario = usuarios.getReferenceById(usuarioId);
        Tarea tarea = new Tarea(
                solicitud.titulo().trim(),
                normalizar(solicitud.descripcion()),
                solicitud.fechaLimite(),
                prioridadOMedia(solicitud),
                propietario);
        tarea.reemplazarEtiquetas(etiquetas.resolver(usuarioId, solicitud.etiquetas()));
        return TareaResponse.desde(tareas.save(tarea), hoy());
    }

    @Transactional
    public TareaResponse actualizar(Long usuarioId, Long id, TareaRequest solicitud) {
        Tarea tarea = buscarDelUsuario(id, usuarioId);
        tarea.actualizar(
                solicitud.titulo().trim(),
                normalizar(solicitud.descripcion()),
                solicitud.fechaLimite(),
                prioridadOMedia(solicitud));
        tarea.reemplazarEtiquetas(etiquetas.resolver(usuarioId, solicitud.etiquetas()));
        return TareaResponse.desde(tarea, hoy());
    }

    @Transactional
    public TareaResponse cambiarEstado(Long usuarioId, Long id, boolean completada) {
        Tarea tarea = buscarDelUsuario(id, usuarioId);
        tarea.marcarCompletada(completada);
        return TareaResponse.desde(tarea, hoy());
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

    private LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    private static Prioridad prioridadOMedia(TareaRequest solicitud) {
        return solicitud.prioridad() != null ? solicitud.prioridad() : Prioridad.MEDIA;
    }

    private static String normalizar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
