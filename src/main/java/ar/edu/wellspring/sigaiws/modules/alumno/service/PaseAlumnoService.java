package ar.edu.wellspring.sigaiws.modules.alumno.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.storage.AlmacenamientoService;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.EstadoAlumno;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.PaseAlumno;
import ar.edu.wellspring.sigaiws.modules.alumno.dto.PaseAlumnoForm;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.PaseAlumnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seccion 5.1.8 (Flujo: Pase de Alumno): cambia el estado a "Inactivo - Pase",
 * registra fecha e institucion destino, genera el PDF con el historial vigente
 * y mantiene el legajo completo archivado y consultable.
 */
@Service
@RequiredArgsConstructor
public class PaseAlumnoService {

    private static final String MODULO = "ALUMNOS";

    private final AlumnoRepository alumnoRepository;
    private final PaseAlumnoRepository paseAlumnoRepository;
    private final AlumnoService alumnoService;
    private final PasePdfGenerator pdfGenerator;
    private final AlmacenamientoService almacenamientoService;
    private final AuditService auditService;

    @Transactional
    public PaseAlumno registrarPase(Long alumnoId, PaseAlumnoForm form) {
        Alumno alumno = alumnoService.obtener(alumnoId);
        if (alumno.getEstadoGlobal() != EstadoAlumno.ACTIVO) {
            throw new BusinessException("Solo se puede registrar el pase de un alumno activo.");
        }

        PaseAlumno pase = new PaseAlumno();
        pase.setAlumno(alumno);
        pase.setFecha(form.getFecha());
        pase.setInstitucionDestino(form.getInstitucionDestino());
        pase.setGeneradoPor(usuarioActual());

        byte[] pdf = pdfGenerator.generar(alumno, pase, alumnoService.trayectoria(alumnoId));
        var guardado = almacenamientoService.guardarBytes(pdf, "alumnos/pases",
                "Pase_" + alumno.getApellido() + "_" + alumno.getNombre() + ".pdf", ".pdf");
        pase.setPdfPath(guardado.pathRelativo());
        pase = paseAlumnoRepository.save(pase);

        alumno.setEstadoGlobal(EstadoAlumno.INACTIVO_PASE);
        alumnoRepository.save(alumno);

        auditService.registrar(MODULO, "REGISTRAR_PASE", "Alumno", alumno.getId(), null,
                "institucionDestino=" + form.getInstitucionDestino());
        return pase;
    }

    private String usuarioActual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "sistema";
    }
}
