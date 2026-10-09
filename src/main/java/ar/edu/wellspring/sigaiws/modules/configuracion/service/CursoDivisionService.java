package ar.edu.wellspring.sigaiws.modules.configuracion.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoCicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CursoDivision;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.Materia;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.MateriaRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Seccion 5.14: alta, edicion y baja de cursos/divisiones (antes solo existian los cargados por migracion). */
@Service
@RequiredArgsConstructor
public class CursoDivisionService {

    private static final String MODULO = "CONFIGURACION";
    private static final int ANIO_MIN = 1;
    private static final int ANIO_MAX = 6;

    private final CursoDivisionRepository cursoDivisionRepository;
    private final MateriaRepository materiaRepository;
    private final AlumnoCicloLectivoRepository alumnoCicloLectivoRepository;
    private final CicloLectivoRepository cicloLectivoRepository;
    private final AuditService auditService;

    public record CursoResumen(CursoDivision curso, int alumnosCicloActivo, int materias) {}

    @Transactional(readOnly = true)
    public List<CursoResumen> listar() {
        List<Materia> materias = materiaRepository.findAllByActivaTrueOrderByNombre();
        return cursoDivisionRepository.todosOrdenados().stream()
                .map(c -> new CursoResumen(c, alumnosEnCicloActivo(c.getId()),
                        (int) materias.stream().filter(m -> m.getCursosDivisiones().contains(c)).count()))
                .toList();
    }

    @Transactional
    public CursoDivision crear(Integer anio, String division, Long copiarMateriasDeId) {
        validarAnio(anio);
        String letra = normalizarLetra(division);
        cursoDivisionRepository.findByAnioAndDivision(anio, letra).ifPresent(existente -> {
            throw new BusinessException(existente.isActiva()
                    ? "Ya existe el curso " + existente.getNombreCompleto() + "."
                    : "El curso " + existente.getNombreCompleto() + " ya existe pero esta desactivado: reactivalo desde el listado.");
        });

        CursoDivision curso = new CursoDivision();
        curso.setAnio(anio);
        curso.setDivision(letra);
        curso = cursoDivisionRepository.save(curso);

        int copiadas = 0;
        if (copiarMateriasDeId != null) {
            CursoDivision origen = obtener(copiarMateriasDeId);
            for (Materia materia : materiaRepository.findAllByActivaTrueOrderByNombre()) {
                if (materia.getCursosDivisiones().contains(origen)) {
                    materia.getCursosDivisiones().add(curso);
                    materiaRepository.save(materia);
                    copiadas++;
                }
            }
        }
        auditService.registrar(MODULO, "CREAR_CURSO_DIVISION", "CursoDivision", curso.getId(), null,
                curso.getNombreCompleto() + (copiadas > 0 ? " (" + copiadas + " materias copiadas)" : ""));
        return curso;
    }

    @Transactional
    public void cambiarDivision(Long id, String division) {
        CursoDivision curso = obtener(id);
        String letra = normalizarLetra(division);
        if (letra.equals(curso.getDivision())) {
            return;
        }
        if (cursoDivisionRepository.findByAnioAndDivision(curso.getAnio(), letra).isPresent()) {
            throw new BusinessException("Ya existe el curso " + curso.getAnio() + "° " + letra + ".");
        }
        String antes = curso.getNombreCompleto();
        curso.setDivision(letra);
        cursoDivisionRepository.save(curso);
        auditService.registrar(MODULO, "EDITAR_CURSO_DIVISION", "CursoDivision", id, antes, curso.getNombreCompleto());
    }

    @Transactional
    public void desactivar(Long id) {
        CursoDivision curso = obtener(id);
        int alumnos = alumnosEnCicloActivo(id);
        if (alumnos > 0) {
            throw new BusinessException("%s tiene %d alumno(s) inscripto(s) en el ciclo lectivo activo. Pasalos a otro curso antes de desactivarlo."
                    .formatted(curso.getNombreCompleto(), alumnos));
        }
        curso.setActiva(false);
        cursoDivisionRepository.save(curso);
        auditService.registrar(MODULO, "DESACTIVAR_CURSO_DIVISION", "CursoDivision", id, null, curso.getNombreCompleto());
    }

    @Transactional
    public void activar(Long id) {
        CursoDivision curso = obtener(id);
        curso.setActiva(true);
        cursoDivisionRepository.save(curso);
        auditService.registrar(MODULO, "ACTIVAR_CURSO_DIVISION", "CursoDivision", id, null, curso.getNombreCompleto());
    }

    /**
     * Borrado definitivo: solo prospera si el curso no tiene datos asociados (alumnos, horarios,
     * notas, etc.). Si los tiene, la base rechaza el borrado y quien llama debe ofrecer desactivarlo.
     */
    @Transactional
    public void eliminar(Long id) {
        CursoDivision curso = obtener(id);
        for (Materia materia : materiaRepository.findAll()) {
            if (materia.getCursosDivisiones().remove(curso)) {
                materiaRepository.save(materia);
            }
        }
        cursoDivisionRepository.delete(curso);
        cursoDivisionRepository.flush();
        auditService.registrar(MODULO, "ELIMINAR_CURSO_DIVISION", "CursoDivision", id, curso.getNombreCompleto(), null);
    }

    @Transactional(readOnly = true)
    public CursoDivision obtener(Long id) {
        return cursoDivisionRepository.findById(id).orElseThrow(() -> NotFoundException.of("CursoDivision", id));
    }

    private int alumnosEnCicloActivo(Long cursoDivisionId) {
        return cicloLectivoRepository.findByActivoTrue()
                .map(ciclo -> alumnoCicloLectivoRepository
                        .findAllByCursoDivision_IdAndCicloLectivo_IdOrderByAlumno_ApellidoAscAlumno_NombreAsc(cursoDivisionId, ciclo.getId())
                        .size())
                .orElse(0);
    }

    private static void validarAnio(Integer anio) {
        if (anio == null || anio < ANIO_MIN || anio > ANIO_MAX) {
            throw new BusinessException("El año debe estar entre " + ANIO_MIN + "° y " + ANIO_MAX + "°.");
        }
    }

    private static String normalizarLetra(String division) {
        String letra = division == null ? "" : division.trim().toUpperCase(Locale.ROOT);
        if (!letra.matches("[A-Z]")) {
            throw new BusinessException("La division debe ser una sola letra (A-Z).");
        }
        return letra;
    }
}
