package com.yarlinson.gestortareas.admin;

import com.yarlinson.gestortareas.usuario.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Consultas de solo lectura para el panel de administración. */
public interface AdminRepository extends Repository<Usuario, Long> {

    @Query(value = """
            select new com.yarlinson.gestortareas.admin.UsuarioAdminResponse(
                u.id, u.nombreUsuario, u.email, u.rol, u.creadoEn,
                (select count(t) from Tarea t where t.usuario.id = u.id))
            from Usuario u
            """,
            countQuery = "select count(u) from Usuario u")
    Page<UsuarioAdminResponse> listarUsuarios(Pageable paginacion);
}
