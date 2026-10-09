package ar.edu.wellspring.sigaiws.common.audit;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Deliberadamente NO extiende JpaRepository con acceso de escritura amplio:
 * solo expone lectura paginada/filtrada (usada por el Superadmin, seccion 3.5)
 * y el guardado interno que usa AuditService. No se expone ningun metodo de
 * borrado ni de actualizacion masiva.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select a from AuditLog a
            where (:usuarioId is null or a.usuarioId = :usuarioId)
              and (:modulo is null or a.modulo = :modulo)
              and (:accion is null or a.accion = :accion)
              and (:desde is null or a.timestamp >= :desde)
              and (:hasta is null or a.timestamp <= :hasta)
            order by a.timestamp desc
            """)
    Page<AuditLog> buscar(@Param("usuarioId") Long usuarioId,
                           @Param("modulo") String modulo,
                           @Param("accion") String accion,
                           @Param("desde") Instant desde,
                           @Param("hasta") Instant hasta,
                           Pageable pageable);

    @Query("""
            select a from AuditLog a
            where (:usuarioId is null or a.usuarioId = :usuarioId)
              and (:modulo is null or a.modulo = :modulo)
              and (:accion is null or a.accion = :accion)
              and (:desde is null or a.timestamp >= :desde)
              and (:hasta is null or a.timestamp <= :hasta)
            order by a.timestamp desc
            """)
    List<AuditLog> buscarTodos(@Param("usuarioId") Long usuarioId,
                                @Param("modulo") String modulo,
                                @Param("accion") String accion,
                                @Param("desde") Instant desde,
                                @Param("hasta") Instant hasta);

    @Query("select distinct a.modulo from AuditLog a order by a.modulo")
    List<String> modulosDistintos();

    @Query("select distinct a.accion from AuditLog a order by a.accion")
    List<String> accionesDistintas();
}
