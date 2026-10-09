package ar.edu.wellspring.sigaiws.modules.mensajeria.repository;

import ar.edu.wellspring.sigaiws.modules.mensajeria.domain.NotificacionSistema;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionSistemaRepository extends JpaRepository<NotificacionSistema, Long> {

    List<NotificacionSistema> findAllByUsuario_IdOrderByCreadoEnDesc(Long usuarioId, Pageable pageable);

    long countByUsuario_IdAndLeidaFalse(Long usuarioId);

    List<NotificacionSistema> findAllByUsuario_IdAndLeidaFalse(Long usuarioId);

    List<NotificacionSistema> findAllByUsuario_IdAndEntidadRelacionadaAndEntidadRelacionadaIdAndLeidaFalse(
            Long usuarioId, String entidadRelacionada, Long entidadRelacionadaId);

    boolean existsByUsuario_IdAndTipoAndEntidadRelacionadaAndEntidadRelacionadaId(
            Long usuarioId, String tipo, String entidadRelacionada, Long entidadRelacionadaId);

    Optional<NotificacionSistema> findFirstByUsuario_IdAndTipoAndEntidadRelacionadaAndEntidadRelacionadaIdAndLeidaFalse(
            Long usuarioId, String tipo, String entidadRelacionada, Long entidadRelacionadaId);
}
