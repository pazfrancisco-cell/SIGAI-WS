package ar.edu.wellspring.sigaiws.modules.calendario.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoTutorRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.service.AlumnoService;
import ar.edu.wellspring.sigaiws.modules.calendario.domain.EventoAcademico;
import ar.edu.wellspring.sigaiws.modules.calendario.dto.EventoAcademicoForm;
import ar.edu.wellspring.sigaiws.modules.calendario.repository.EventoAcademicoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.horario.repository.AsignacionDocenteRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Calendario academico (institucional o por curso/division): carga solo admin, lectura para todos los roles. */
@Service
@RequiredArgsConstructor
public class EventoAcademicoService {

    private static final String MODULO = "CALENDARIO";

    private final EventoAcademicoRepository eventoAcademicoRepository;
    private final CursoDivisionRepository cursoDivisionRepository;
    private final CicloLectivoRepository cicloLectivoRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final AlumnoTutorRepository alumnoTutorRepository;
    private final AlumnoService alumnoService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<EventoAcademico> listarTodos() {
        return eventoAcademicoRepository.findAllByCicloLectivo_IdOrderByFechaInicio(cicloActivo().getId());
    }

    /** Institucionales + eventos de los cursos/divisiones donde el docente tiene alguna materia asignada. */
    @Transactional(readOnly = true)
    public List<EventoAcademico> listarParaDocente(Long docenteId) {
        CicloLectivo activo = cicloActivo();
        Set<Long> cursoIds = new LinkedHashSet<>();
        for (var asignacion : asignacionDocenteRepository.findAllByDocente_IdAndCicloLectivo_Id(docenteId, activo.getId())) {
            cursoIds.add(asignacion.getCursoDivision().getId());
        }
        return listarInstitucionalesYDeCursos(activo.getId(), cursoIds);
    }

    /** Institucionales + eventos del curso/division actual de cada hijo vinculado al tutor. */
    @Transactional(readOnly = true)
    public List<EventoAcademico> listarParaTutor(Long usuarioId) {
        CicloLectivo activo = cicloActivo();
        Set<Long> cursoIds = new LinkedHashSet<>();
        for (var vinculo : alumnoTutorRepository.findAllByTutor_UsuarioId(usuarioId)) {
            try {
                cursoIds.add(alumnoService.cicloActualDe(vinculo.getAlumno().getId()).getCursoDivision().getId());
            } catch (BusinessException ignored) {
                // el hijo no tiene trayectoria en el ciclo activo
            }
        }
        return listarInstitucionalesYDeCursos(activo.getId(), cursoIds);
    }

    private List<EventoAcademico> listarInstitucionalesYDeCursos(Long cicloId, Set<Long> cursoIds) {
        List<EventoAcademico> resultado = new java.util.ArrayList<>(
                eventoAcademicoRepository.findAllByCicloLectivo_IdAndCursoDivisionIsNullOrderByFechaInicio(cicloId));
        if (!cursoIds.isEmpty()) {
            resultado.addAll(eventoAcademicoRepository.findAllByCicloYCursos(cicloId, cursoIds.stream().toList()));
        }
        resultado.sort(java.util.Comparator.comparing(EventoAcademico::getFechaInicio));
        return resultado;
    }

    @Transactional
    public EventoAcademico crear(EventoAcademicoForm form, Usuario creadoPor) {
        EventoAcademico evento = new EventoAcademico();
        aplicarForm(evento, form);
        evento.setCicloLectivo(cicloActivo());
        evento.setCreadoPor(creadoPor);
        evento = eventoAcademicoRepository.save(evento);
        auditService.registrar(MODULO, "CREAR_EVENTO", "EventoAcademico", evento.getId(), null, evento.getTitulo());
        return evento;
    }

    @Transactional
    public EventoAcademico editar(Long id, EventoAcademicoForm form) {
        EventoAcademico evento = obtener(id);
        aplicarForm(evento, form);
        evento = eventoAcademicoRepository.save(evento);
        auditService.registrar(MODULO, "EDITAR_EVENTO", "EventoAcademico", evento.getId(), null, evento.getTitulo());
        return evento;
    }

    @Transactional
    public void eliminar(Long id) {
        EventoAcademico evento = obtener(id);
        eventoAcademicoRepository.delete(evento);
        auditService.registrar(MODULO, "ELIMINAR_EVENTO", "EventoAcademico", id, evento.getTitulo(), null);
    }

    @Transactional(readOnly = true)
    public EventoAcademico obtener(Long id) {
        return eventoAcademicoRepository.findById(id).orElseThrow(() -> NotFoundException.of("EventoAcademico", id));
    }

    private void aplicarForm(EventoAcademico evento, EventoAcademicoForm form) {
        if (form.getFechaFin() != null && form.getFechaFin().isBefore(form.getFechaInicio())) {
            throw new BusinessException("La fecha de fin no puede ser anterior a la fecha de inicio.");
        }
        evento.setTitulo(form.getTitulo());
        evento.setDescripcion(form.getDescripcion());
        evento.setTipo(form.getTipo());
        evento.setFechaInicio(form.getFechaInicio());
        evento.setFechaFin(form.getFechaFin());
        evento.setCursoDivision(form.getCursoDivisionId() != null
                ? cursoDivisionRepository.findById(form.getCursoDivisionId())
                        .orElseThrow(() -> NotFoundException.of("CursoDivision", form.getCursoDivisionId()))
                : null);
    }

    private CicloLectivo cicloActivo() {
        return cicloLectivoRepository.findByActivoTrue()
                .orElseThrow(() -> new BusinessException("No hay un ciclo lectivo activo configurado."));
    }
}
