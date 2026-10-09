package ar.edu.wellspring.sigaiws.modules.edfisica.repository;

import ar.edu.wellspring.sigaiws.modules.edfisica.domain.GrupoEducacionFisica;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrupoEducacionFisicaRepository extends JpaRepository<GrupoEducacionFisica, Long> {
    List<GrupoEducacionFisica> findAllByCicloLectivo_IdAndActivoTrue(Long cicloLectivoId);
    List<GrupoEducacionFisica> findAllByDocente_IdAndCicloLectivo_Id(Long docenteId, Long cicloLectivoId);
    List<GrupoEducacionFisica> findAllByPruebasDefiniciones_Id(Long definicionId);
}
