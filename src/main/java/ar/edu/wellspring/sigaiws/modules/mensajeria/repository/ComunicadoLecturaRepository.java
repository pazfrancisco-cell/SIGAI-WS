package ar.edu.wellspring.sigaiws.modules.mensajeria.repository;

import ar.edu.wellspring.sigaiws.modules.mensajeria.domain.ComunicadoLectura;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComunicadoLecturaRepository extends JpaRepository<ComunicadoLectura, Long> {
    Optional<ComunicadoLectura> findByComunicado_IdAndUsuario_Id(Long comunicadoId, Long usuarioId);
    List<ComunicadoLectura> findAllByComunicado_IdOrderByLeidoEnAsc(Long comunicadoId);
    long countByComunicado_Id(Long comunicadoId);
}
