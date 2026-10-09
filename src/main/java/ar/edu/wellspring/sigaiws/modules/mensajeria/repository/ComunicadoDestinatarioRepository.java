package ar.edu.wellspring.sigaiws.modules.mensajeria.repository;

import ar.edu.wellspring.sigaiws.modules.mensajeria.domain.ComunicadoDestinatario;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComunicadoDestinatarioRepository extends JpaRepository<ComunicadoDestinatario, Long> {
    List<ComunicadoDestinatario> findAllByComunicado_Id(Long comunicadoId);

    List<ComunicadoDestinatario> findAllByTipoDestinatario(ComunicadoDestinatario.TipoDestinatario tipo);

    List<ComunicadoDestinatario> findAllByTipoDestinatarioAndCursoDivision_IdIn(
            ComunicadoDestinatario.TipoDestinatario tipo, Collection<Long> cursoDivisionIds);

    List<ComunicadoDestinatario> findAllByTipoDestinatarioAndAlumno_IdIn(
            ComunicadoDestinatario.TipoDestinatario tipo, Collection<Long> alumnoIds);
}
