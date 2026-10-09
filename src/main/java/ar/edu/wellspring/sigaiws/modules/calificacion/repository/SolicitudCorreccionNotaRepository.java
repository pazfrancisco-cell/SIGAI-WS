package ar.edu.wellspring.sigaiws.modules.calificacion.repository;

import ar.edu.wellspring.sigaiws.modules.calificacion.domain.SolicitudCorreccionNota;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudCorreccionNotaRepository extends JpaRepository<SolicitudCorreccionNota, Long> {
    List<SolicitudCorreccionNota> findAllByEstadoOrderBySolicitadoEnDesc(SolicitudCorreccionNota.Estado estado);
    List<SolicitudCorreccionNota> findAllBySolicitadoPor_IdOrderBySolicitadoEnDesc(Long usuarioId);
    void deleteAllByNota_IdIn(List<Long> notaIds);
}
