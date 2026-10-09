package ar.edu.wellspring.sigaiws.modules.horario.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.MateriaRepository;
import ar.edu.wellspring.sigaiws.modules.horario.domain.AsignacionDocente;
import ar.edu.wellspring.sigaiws.modules.horario.dto.AsignacionDocenteForm;
import ar.edu.wellspring.sigaiws.modules.horario.repository.AsignacionDocenteRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.UsuarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Seccion 5.5.5 (Asignacion Docente). */
@Service
@RequiredArgsConstructor
public class AsignacionDocenteService {

    private static final String MODULO = "HORARIOS";

    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final MateriaRepository materiaRepository;
    private final CursoDivisionRepository cursoDivisionRepository;
    private final CicloLectivoRepository cicloLectivoRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<AsignacionDocente> listarDelCicloActivo() {
        CicloLectivo activo = cicloActivo();
        return asignacionDocenteRepository.findAllByCicloLectivo_IdOrderByDocente_NombreCompleto(activo.getId());
    }

    @Transactional
    public AsignacionDocente crear(AsignacionDocenteForm form) {
        CicloLectivo activo = cicloActivo();
        if (asignacionDocenteRepository.existsByDocente_IdAndMateria_IdAndCursoDivision_IdAndCicloLectivo_Id(
                form.getDocenteId(), form.getMateriaId(), form.getCursoDivisionId(), activo.getId())) {
            throw new BusinessException("Esa asignacion ya existe para el ciclo lectivo activo.");
        }

        AsignacionDocente asignacion = new AsignacionDocente();
        asignacion.setDocente(usuarioRepository.findById(form.getDocenteId())
                .orElseThrow(() -> NotFoundException.of("Usuario", form.getDocenteId())));
        asignacion.setMateria(materiaRepository.findById(form.getMateriaId())
                .orElseThrow(() -> NotFoundException.of("Materia", form.getMateriaId())));
        asignacion.setCursoDivision(cursoDivisionRepository.findById(form.getCursoDivisionId())
                .orElseThrow(() -> NotFoundException.of("CursoDivision", form.getCursoDivisionId())));
        asignacion.setCicloLectivo(activo);
        asignacion = asignacionDocenteRepository.save(asignacion);

        auditService.registrar(MODULO, "CREAR_ASIGNACION_DOCENTE", "AsignacionDocente", asignacion.getId(), null,
                "docente=" + asignacion.getDocente().getEmail() + ", materia=" + asignacion.getMateria().getNombre());
        return asignacion;
    }

    @Transactional
    public void eliminar(Long id) {
        AsignacionDocente asignacion = asignacionDocenteRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("AsignacionDocente", id));
        asignacionDocenteRepository.delete(asignacion);
        auditService.registrar(MODULO, "ELIMINAR_ASIGNACION_DOCENTE", "AsignacionDocente", id, null, null);
    }

    private CicloLectivo cicloActivo() {
        return cicloLectivoRepository.findByActivoTrue()
                .orElseThrow(() -> new BusinessException("No hay un ciclo lectivo activo configurado."));
    }
}
