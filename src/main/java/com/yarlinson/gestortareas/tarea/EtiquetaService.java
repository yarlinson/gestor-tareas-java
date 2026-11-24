package com.yarlinson.gestortareas.tarea;

import com.yarlinson.gestortareas.common.RecursoNoEncontradoException;
import com.yarlinson.gestortareas.usuario.Usuario;
import com.yarlinson.gestortareas.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EtiquetaService {

    private final EtiquetaRepository etiquetas;
    private final UsuarioRepository usuarios;

    public EtiquetaService(EtiquetaRepository etiquetas, UsuarioRepository usuarios) {
        this.etiquetas = etiquetas;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<EtiquetaResponse> listar(Long usuarioId) {
        return etiquetas.listarConConteo(usuarioId);
    }

    /** Elimina la etiqueta y la quita de todas las tareas que la tenían (lo hace la clave foránea). */
    @Transactional
    public void eliminar(Long usuarioId, Long id) {
        Etiqueta etiqueta = etiquetas.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Etiqueta " + id + " no encontrada"));
        etiquetas.delete(etiqueta);
    }

    /**
     * Devuelve las etiquetas del usuario con esos nombres, creando las que todavía no existan.
     * Los nombres se normalizan (sin espacios sobrantes y en minúsculas) y se descartan repetidos.
     */
    @Transactional
    public Set<Etiqueta> resolver(Long usuarioId, Collection<String> nombresSolicitados) {
        Set<String> nombres = normalizar(nombresSolicitados);
        if (nombres.isEmpty()) {
            return new LinkedHashSet<>();
        }

        Map<String, Etiqueta> existentes = etiquetas.findByUsuarioIdAndNombreIn(usuarioId, nombres).stream()
                .collect(Collectors.toMap(Etiqueta::getNombre, Function.identity()));
        Usuario propietario = usuarios.getReferenceById(usuarioId);

        Set<Etiqueta> resultado = new LinkedHashSet<>();
        for (String nombre : nombres) {
            resultado.add(existentes.computeIfAbsent(nombre, n -> etiquetas.save(new Etiqueta(n, propietario))));
        }
        return resultado;
    }

    static Set<String> normalizar(Collection<String> nombres) {
        if (nombres == null) {
            return new LinkedHashSet<>();
        }
        return nombres.stream()
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .map(nombre -> nombre.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
