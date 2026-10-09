package ar.edu.wellspring.sigaiws.modules.edfisica.repository;

import ar.edu.wellspring.sigaiws.modules.edfisica.domain.GrupoEfAlumno;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface GrupoEfAlumnoRepository extends JpaRepository<GrupoEfAlumno, Long> {
    List<GrupoEfAlumno> findAllByGrupo_IdAndCuatrimestre(Long grupoId, Integer cuatrimestre);

    /**
     * Bulk delete (no la derivada por entidad): al editar el plantel de un grupo se borra y
     * se vuelve a insertar en la misma transaccion, y algunos alumnos se repiten entre el
     * plantel viejo y el nuevo. Con un delete "por entidad" Hibernate reordena las acciones
     * (inserta antes de borrar) y el INSERT choca contra el UNIQUE(grupo_id, alumno_id,
     * cuatrimestre) porque la fila vieja todavia no se borro. El bulk delete se ejecuta de
     * inmediato contra la base, sin ese reordenamiento.
     */
    @Modifying
    @Query("delete from GrupoEfAlumno g where g.grupo.id = :grupoId and g.cuatrimestre = :cuatrimestre")
    void deleteAllByGrupo_IdAndCuatrimestre(Long grupoId, Integer cuatrimestre);
}
