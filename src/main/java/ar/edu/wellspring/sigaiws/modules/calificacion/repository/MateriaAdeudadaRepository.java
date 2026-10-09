package ar.edu.wellspring.sigaiws.modules.calificacion.repository;

import ar.edu.wellspring.sigaiws.modules.calificacion.domain.MateriaAdeudada;
import ar.edu.wellspring.sigaiws.modules.calificacion.domain.TurnoMesa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MateriaAdeudadaRepository extends JpaRepository<MateriaAdeudada, Long> {

    Optional<MateriaAdeudada> findByAlumno_IdAndMateria_IdAndCicloLectivo_Id(Long alumnoId, Long materiaId, Long cicloLectivoId);

    List<MateriaAdeudada> findAllByCicloLectivo_Id(Long cicloLectivoId);

    List<MateriaAdeudada> findAllByAlumno_IdAndEstado(Long alumnoId, MateriaAdeudada.Estado estado);

    List<MateriaAdeudada> findAllByAlumno_IdAndCicloLectivo_Id(Long alumnoId, Long cicloLectivoId);

    @Query("""
            select m from MateriaAdeudada m
            where m.alumno.id = :alumnoId
            order by m.cicloLectivo.anio desc, m.materia.nombre
            """)
    List<MateriaAdeudada> deAlumno(@Param("alumnoId") Long alumnoId);

    /** Lo que se puede rendir en una mesa: pendientes (y las que ya se cargaron en ese turno, para corregirlas). */
    default List<MateriaAdeudada> paraMesa(TurnoMesa turno, Integer anio, Long materiaId, Integer anioCurso) {
        return paraMesa(turno, anio, materiaId, anioCurso, MateriaAdeudada.Estado.PENDIENTE, MateriaAdeudada.Estado.RECURSADA);
    }

    @Query("""
            select m from MateriaAdeudada m
            where (m.estado = :pendiente
                   or exists (select n from NotaMesa n where n.materiaAdeudada = m and n.turno = :turno and n.anio = :anio))
              and m.estado <> :recursada
              and (:materiaId is null or m.materia.id = :materiaId)
              and (:anioCurso is null or m.cursoDivision.anio = :anioCurso)
            order by m.materia.nombre, m.cursoDivision.anio, m.alumno.apellido, m.alumno.nombre
            """)
    List<MateriaAdeudada> paraMesa(@Param("turno") TurnoMesa turno,
                                   @Param("anio") Integer anio,
                                   @Param("materiaId") Long materiaId,
                                   @Param("anioCurso") Integer anioCurso,
                                   @Param("pendiente") MateriaAdeudada.Estado pendiente,
                                   @Param("recursada") MateriaAdeudada.Estado recursada);
}
