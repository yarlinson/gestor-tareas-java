package com.yarlinson.gestortareas.admin;

import com.yarlinson.gestortareas.tarea.TareaRepository;
import com.yarlinson.gestortareas.tarea.TareaSpecs;
import com.yarlinson.gestortareas.usuario.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class AdminService {

    private final AdminRepository admin;
    private final UsuarioRepository usuarios;
    private final TareaRepository tareas;
    private final Clock reloj;

    public AdminService(AdminRepository admin, UsuarioRepository usuarios, TareaRepository tareas, Clock reloj) {
        this.admin = admin;
        this.usuarios = usuarios;
        this.tareas = tareas;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Page<UsuarioAdminResponse> listarUsuarios(Pageable paginacion) {
        return admin.listarUsuarios(paginacion);
    }

    @Transactional(readOnly = true)
    public EstadisticasResponse estadisticas() {
        long totalTareas = tareas.count();
        long completadas = tareas.count(TareaSpecs.conEstado(true));
        long vencidas = tareas.count(TareaSpecs.vencidas(true, LocalDate.now(reloj)));
        return new EstadisticasResponse(usuarios.count(), totalTareas, completadas, totalTareas - completadas, vencidas);
    }
}
