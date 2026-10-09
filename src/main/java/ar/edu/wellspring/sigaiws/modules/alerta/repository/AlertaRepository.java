package ar.edu.wellspring.sigaiws.modules.alerta.repository;

import ar.edu.wellspring.sigaiws.modules.alerta.domain.Alerta;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {
    List<Alerta> findAllByEstadoOrderByPrioridadDescGeneradaEnDesc(Alerta.Estado estado);
    List<Alerta> findAllByAlumno_IdOrderByGeneradaEnDesc(Long alumnoId);
    List<Alerta> findAllByOrderByGeneradaEnDesc();

    /** Idempotencia de alertas generadas automaticamente por ciclo lectivo (ej. apto fisico vencido): no hay
     * columna de ciclo lectivo en Alerta, asi que se usa la fecha de inicio del ciclo activo como corte. */
    boolean existsByTipoAndAlumno_IdAndGeneradaEnAfter(String tipo, Long alumnoId, LocalDateTime generadaEnDesde);

    /** Alerta activa (no resuelta) del mismo tipo para el mismo alumno y la misma entidad (ej. misma materia). */
    Optional<Alerta> findFirstByTipoAndAlumno_IdAndEntidadRelacionadaAndEntidadRelacionadaIdAndEstadoNot(
            String tipo, Long alumnoId, String entidadRelacionada, Long entidadRelacionadaId, Alerta.Estado estado);

    Optional<Alerta> findFirstByTipoAndEntidadRelacionadaAndEntidadRelacionadaIdAndEstadoNot(
            String tipo, String entidadRelacionada, Long entidadRelacionadaId, Alerta.Estado estado);

    /** Para avisos que deben generarse una unica vez, aunque despues se resuelvan. */
    boolean existsByTipoAndEntidadRelacionadaAndEntidadRelacionadaId(String tipo, String entidadRelacionada, Long entidadRelacionadaId);
}
