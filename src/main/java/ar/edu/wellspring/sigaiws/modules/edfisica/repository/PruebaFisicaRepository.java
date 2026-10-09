package ar.edu.wellspring.sigaiws.modules.edfisica.repository;

import ar.edu.wellspring.sigaiws.modules.edfisica.domain.PruebaFisica;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PruebaFisicaRepository extends JpaRepository<PruebaFisica, Long> {
    List<PruebaFisica> findAllByGrupo_IdAndCuatrimestre(Long grupoId, Integer cuatrimestre);

    /** Pruebas anteriores de la misma definicion en el mismo grupo (seccion 5.7.4: "progreso individual comparando con pruebas anteriores del mismo tipo"). */
    List<PruebaFisica> findAllByGrupo_IdAndDefinicion_IdAndFechaBeforeOrderByFechaDesc(Long grupoId, Long definicionId, LocalDate fecha);

    List<PruebaFisica> findAllByDefinicion_Id(Long definicionId);
}
