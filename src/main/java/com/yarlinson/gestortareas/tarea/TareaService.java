package com.yarlinson.gestortareas.tarea;

import com.yarlinson.gestortareas.common.RecursoNoEncontradoException;
import com.yarlinson.gestortareas.usuario.Usuario;
import com.yarlinson.gestortareas.usuario.UsuarioRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import static com.yarlinson.gestortareas.tarea.TareaSpecs.conEstado;
import static com.yarlinson.gestortareas.tarea.TareaSpecs.conEtiqueta;
import static com.yarlinson.gestortareas.tarea.TareaSpecs.conFechaEntre;
import static com.yarlinson.gestortareas.tarea.TareaSpecs.conPrioridad;
import static com.yarlinson.gestortareas.tarea.TareaSpecs.contieneTexto;
import static com.yarlinson.gestortareas.tarea.TareaSpecs.deUsuario;
import static com.yarlinson.gestortareas.tarea.TareaSpecs.sinFecha;
import static com.yarlinson.gestortareas.tarea.TareaSpecs.vencidas;

@Service
public class TareaService {

    private final TareaRepository tareas;
    private final UsuarioRepository usuarios;
    private final EtiquetaService etiquetas;
    private final Clock reloj;
    private final ApplicationEventPublisher eventos;

    public TareaService(
            TareaRepository tareas,
            UsuarioRepository usuarios,
            EtiquetaService etiquetas,
            Clock reloj,
            ApplicationEventPublisher eventos) {
        this.tareas = tareas;
        this.usuarios = usuarios;
        this.etiquetas = etiquetas;
        this.reloj = reloj;
        this.eventos = eventos;
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
                vencidas(filtro.vencidas(), hoy),
                conFechaEntre(filtro.desde(), filtro.hasta()),
                sinFecha(filtro.sinFecha()));
        return tareas.findAll(criterios, pageable).map(tarea -> TareaResponse.desde(tarea, hoy));
    }

    /** Totales para un panel de resumen. "Hoy" se calcula en la zona horaria configurada. */
    @Transactional(readOnly = true)
    public ResumenTareas resumen(Long usuarioId) {
        LocalDate hoy = hoy();
        Specification<Tarea> propias = deUsuario(usuarioId);
        Specification<Tarea> pendientes = propias.and(conEstado(false));

        long total = tareas.count(propias);
        long cantidadPendientes = tareas.count(pendientes);
        return new ResumenTareas(
                total,
                cantidadPendientes,
                total - cantidadPendientes,
                tareas.count(propias.and(vencidas(true, hoy))),
                tareas.count(pendientes.and(conFechaEntre(hoy, hoy))),
                tareas.count(pendientes.and(conFechaEntre(hoy.plusDays(1), hoy.plusDays(7)))),
                tareas.count(pendientes.and(sinFecha(true))),
                tareas.count(pendientes.and(conPrioridad(Prioridad.ALTA))),
                tareas.count(pendientes.and(conPrioridad(Prioridad.MEDIA))),
                tareas.count(pendientes.and(conPrioridad(Prioridad.BAJA))));
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
        LocalDate fechaAnterior = tarea.getFechaLimite();
        tarea.actualizar(
                solicitud.titulo().trim(),
                normalizar(solicitud.descripcion()),
                solicitud.fechaLimite(),
                prioridadOMedia(solicitud));
        tarea.reemplazarEtiquetas(etiquetas.resolver(usuarioId, solicitud.etiquetas()));
        if (!Objects.equals(fechaAnterior, tarea.getFechaLimite())) {
            eventos.publishEvent(new FechaLimiteCambiada(tarea.getId()));
        }
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
