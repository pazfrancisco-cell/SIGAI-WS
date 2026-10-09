package ar.edu.wellspring.sigaiws.modules.alumno.repository;

import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlumnoRepository extends JpaRepository<Alumno, Long> {

    boolean existsByDniHash(String dniHash);

    Optional<Alumno> findByDniHash(String dniHash);

    /**
     * Filtro combinado del listado (seccion 5.1.3): texto libre (nombre/apellido -
     * el DNI esta cifrado y se busca aparte por dniHash), año/division vigentes
     * (via alumno_ciclo_lectivo del ciclo activo) y estado.
     */
    @Query("""
            select distinct a from Alumno a
            left join AlumnoCicloLectivo acl on acl.alumno = a and acl.cicloLectivo.id = :cicloLectivoId
            where (:texto is null or lower(a.apellido) like lower(concat('%', :texto, '%'))
                                   or lower(a.nombre) like lower(concat('%', :texto, '%')))
              and (:estado is null or a.estadoGlobal = :estado)
              and (:cursoDivisionId is null or acl.cursoDivision.id = :cursoDivisionId)
              and (:condicionLibre is null or acl.condicionLibre = :condicionLibre)
            order by a.apellido, a.nombre
            """)
    Page<Alumno> buscar(@Param("texto") String texto,
                         @Param("estado") ar.edu.wellspring.sigaiws.modules.alumno.domain.EstadoAlumno estado,
                         @Param("cursoDivisionId") Long cursoDivisionId,
                         @Param("condicionLibre") Boolean condicionLibre,
                         @Param("cicloLectivoId") Long cicloLectivoId,
                         Pageable pageable);

    /** Igual que {@link #buscar}, pero restringido a un conjunto de cursos/divisiones (vista Docente, seccion 5.1.2). */
    @Query("""
            select distinct a from Alumno a
            left join AlumnoCicloLectivo acl on acl.alumno = a and acl.cicloLectivo.id = :cicloLectivoId
            where (:texto is null or lower(a.apellido) like lower(concat('%', :texto, '%'))
                                   or lower(a.nombre) like lower(concat('%', :texto, '%')))
              and (:estado is null or a.estadoGlobal = :estado)
              and (:cursoDivisionId is null or acl.cursoDivision.id = :cursoDivisionId)
              and (:condicionLibre is null or acl.condicionLibre = :condicionLibre)
              and acl.cursoDivision.id in :cursoIdsPermitidos
            order by a.apellido, a.nombre
            """)
    Page<Alumno> buscarEntreCursos(@Param("texto") String texto,
                                    @Param("estado") ar.edu.wellspring.sigaiws.modules.alumno.domain.EstadoAlumno estado,
                                    @Param("cursoDivisionId") Long cursoDivisionId,
                                    @Param("condicionLibre") Boolean condicionLibre,
                                    @Param("cicloLectivoId") Long cicloLectivoId,
                                    @Param("cursoIdsPermitidos") List<Long> cursoIdsPermitidos,
                                    Pageable pageable);
}
