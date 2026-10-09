package ar.edu.wellspring.sigaiws.modules.disciplina.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.common.notification.EmailService;
import ar.edu.wellspring.sigaiws.modules.mensajeria.service.NotificacionService;
import ar.edu.wellspring.sigaiws.modules.alerta.domain.Alerta;
import ar.edu.wellspring.sigaiws.modules.alerta.service.AlertaService;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoCicloLectivo;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoTutor;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoCicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoTutorRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.disciplina.domain.ActaDisciplinaria;
import ar.edu.wellspring.sigaiws.modules.disciplina.domain.FirmaDisciplinaria;
import ar.edu.wellspring.sigaiws.modules.disciplina.domain.ObservacionDisciplinaria;
import ar.edu.wellspring.sigaiws.modules.disciplina.dto.ActaForm;
import ar.edu.wellspring.sigaiws.modules.disciplina.dto.ObservacionForm;
import ar.edu.wellspring.sigaiws.modules.disciplina.repository.ActaDisciplinariaRepository;
import ar.edu.wellspring.sigaiws.modules.disciplina.repository.FirmaDisciplinariaRepository;
import ar.edu.wellspring.sigaiws.modules.disciplina.repository.ObservacionDisciplinariaRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Modulo 5.4 (Disciplina). Escala: Observacion -> Firma (auto c/5) -> Acta Acuerdo (3 firmas) -> Acta Matriculacion (5 firmas). */
@Service
@RequiredArgsConstructor
public class DisciplinaService {

    private static final String MODULO = "DISCIPLINA";
    private static final int OBS_POR_FIRMA = 5;
    private static final int FIRMAS_ACTA_ACUERDO = 3;
    private static final int FIRMAS_ACTA_MATRICULACION = 5;

    private final ObservacionDisciplinariaRepository observacionRepository;
    private final FirmaDisciplinariaRepository firmaRepository;
    private final ActaDisciplinariaRepository actaRepository;
    private final AlumnoRepository alumnoRepository;
    private final AlumnoCicloLectivoRepository alumnoCicloLectivoRepository;
    private final AlumnoTutorRepository alumnoTutorRepository;
    private final CicloLectivoRepository cicloLectivoRepository;
    private final AlertaService alertaService;
    private final EmailService emailService;
    private final NotificacionService notificacionService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ObservacionDisciplinaria> historialDe(Long alumnoId) {
        return observacionRepository.findAllByAlumno_IdAndCicloLectivo_IdOrderByFechaDesc(alumnoId, cicloActivo().getId());
    }

    @Transactional(readOnly = true)
    public List<FirmaDisciplinaria> firmasDe(Long alumnoId) {
        return firmaRepository.findAllByAlumno_IdAndCicloLectivo_IdOrderByNumero(alumnoId, cicloActivo().getId());
    }

    /** Resumen de observaciones y firmas acumuladas por alumno de una division, en el ciclo lectivo activo. */
    @Transactional(readOnly = true)
    public List<AlumnoCicloLectivo> resumenPorDivision(Long cursoDivisionId) {
        CicloLectivo activo = cicloActivo();
        return alumnoCicloLectivoRepository.findAllByCursoDivision_IdAndCicloLectivo_IdOrderByAlumno_ApellidoAscAlumno_NombreAsc(
                cursoDivisionId, activo.getId());
    }

    /** Seccion 6.3 (Flujo: Generacion de Firma Disciplinaria Automatica). */
    @Transactional
    public ObservacionDisciplinaria registrarObservacion(ObservacionForm form, Usuario registradoPor) {
        CicloLectivo activo = cicloActivo();
        Alumno alumno = alumnoRepository.findById(form.getAlumnoId())
                .orElseThrow(() -> NotFoundException.of("Alumno", form.getAlumnoId()));

        ObservacionDisciplinaria observacion = new ObservacionDisciplinaria();
        observacion.setAlumno(alumno);
        observacion.setCicloLectivo(activo);
        observacion.setFecha(form.getFecha());
        observacion.setDescripcion(form.getDescripcion());
        observacion.setCategoria(form.getCategoria());
        observacion.setFirmantesAdicionales(form.getFirmantesAdicionales());
        observacion.setRegistradoPor(registradoPor);
        observacion = observacionRepository.save(observacion);

        AlumnoCicloLectivo acl = alumnoCicloLectivoRepository.findByAlumno_IdAndCicloLectivo_Id(alumno.getId(), activo.getId())
                .orElseThrow(() -> new BusinessException("El alumno no tiene trayectoria en el ciclo lectivo activo."));
        acl.setObservacionesAcumuladas(acl.getObservacionesAcumuladas() + 1);
        alumnoCicloLectivoRepository.save(acl);

        auditService.registrar(MODULO, "REGISTRAR_OBSERVACION", "ObservacionDisciplinaria", observacion.getId(),
                null, "alumno=" + alumno.getNombreCompleto());

        if (acl.getObservacionesAcumuladas() % OBS_POR_FIRMA == 0) {
            generarFirma(alumno, activo, acl, observacion);
        }
        return observacion;
    }

    /**
     * Seccion 5.4.3: "Solo el Administrativo puede eliminar o modificar una
     * observacion ya registrada, con registro en auditoria." No recalcula
     * retroactivamente las Firmas Disciplinarias ya generadas: una firma que
     * ya fue notificada a tutores y Direccion no se "deshace" al editar o
     * borrar una de las observaciones que la originaron.
     */
    @Transactional
    public ObservacionDisciplinaria editarObservacion(Long id, ObservacionForm form) {
        ObservacionDisciplinaria observacion = observacionRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("ObservacionDisciplinaria", id));
        String antes = "fecha=%s, categoria=%s, descripcion=%s".formatted(
                observacion.getFecha(), observacion.getCategoria(), observacion.getDescripcion());

        observacion.setFecha(form.getFecha());
        observacion.setDescripcion(form.getDescripcion());
        observacion.setCategoria(form.getCategoria());
        observacion.setFirmantesAdicionales(form.getFirmantesAdicionales());
        observacion = observacionRepository.save(observacion);

        auditService.registrar(MODULO, "EDITAR_OBSERVACION", "ObservacionDisciplinaria", observacion.getId(),
                antes, "fecha=%s, categoria=%s, descripcion=%s".formatted(
                        observacion.getFecha(), observacion.getCategoria(), observacion.getDescripcion()));
        return observacion;
    }

    @Transactional
    public void eliminarObservacion(Long id) {
        ObservacionDisciplinaria observacion = observacionRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("ObservacionDisciplinaria", id));
        Alumno alumno = observacion.getAlumno();
        CicloLectivo ciclo = observacion.getCicloLectivo();
        String resumen = "alumno=%s, fecha=%s, descripcion=%s".formatted(
                alumno.getNombreCompleto(), observacion.getFecha(), observacion.getDescripcion());

        observacionRepository.delete(observacion);

        alumnoCicloLectivoRepository.findByAlumno_IdAndCicloLectivo_Id(alumno.getId(), ciclo.getId())
                .ifPresent(acl -> {
                    long restantes = observacionRepository.countByAlumno_IdAndCicloLectivo_Id(alumno.getId(), ciclo.getId());
                    acl.setObservacionesAcumuladas((int) restantes);
                    alumnoCicloLectivoRepository.save(acl);
                });

        auditService.registrar(MODULO, "ELIMINAR_OBSERVACION", "ObservacionDisciplinaria", id, resumen, null);
    }

    @Transactional(readOnly = true)
    public ObservacionDisciplinaria obtenerObservacion(Long id) {
        return observacionRepository.findById(id).orElseThrow(() -> NotFoundException.of("ObservacionDisciplinaria", id));
    }

    private void generarFirma(Alumno alumno, CicloLectivo activo, AlumnoCicloLectivo acl, ObservacionDisciplinaria disparadora) {
        int numeroFirma = acl.getObservacionesAcumuladas() / OBS_POR_FIRMA;

        FirmaDisciplinaria firma = new FirmaDisciplinaria();
        firma.setAlumno(alumno);
        firma.setCicloLectivo(activo);
        firma.setNumero(numeroFirma);
        firma.setObservacionDisparadora(disparadora);
        firmaRepository.save(firma);

        acl.setFirmasAcumuladas(numeroFirma);
        alumnoCicloLectivoRepository.save(acl);

        String descripcion = "%s acumuló %d observaciones. Se generó automáticamente la Firma %d."
                .formatted(alumno.getNombreCompleto(), acl.getObservacionesAcumuladas(), numeroFirma);
        alertaService.crear("Firma disciplinaria generada", Alerta.Prioridad.ALTA, alumno, descripcion);
        notificarTutoresFirma(alumno, numeroFirma);

        auditService.registrar(MODULO, "GENERAR_FIRMA_AUTOMATICA", "FirmaDisciplinaria", firma.getId(), null,
                "numero=" + numeroFirma);

        if (numeroFirma == FIRMAS_ACTA_ACUERDO) {
            alertaService.crear("Acta de Acuerdo requerida", Alerta.Prioridad.ALTA, alumno,
                    "%s alcanzó %d firmas. Corresponde generar el Acta de Acuerdo.".formatted(alumno.getNombreCompleto(), FIRMAS_ACTA_ACUERDO));
        } else if (numeroFirma == FIRMAS_ACTA_MATRICULACION) {
            alertaService.crear("Acta de Matriculacion requerida", Alerta.Prioridad.CRITICA, alumno,
                    "%s alcanzó %d firmas. Corresponde generar el Acta de Matriculacion.".formatted(alumno.getNombreCompleto(), FIRMAS_ACTA_MATRICULACION));
        }
    }

    @Transactional
    public ActaDisciplinaria generarActa(ActaForm form, Usuario generadaPor) {
        CicloLectivo activo = cicloActivo();
        Alumno alumno = alumnoRepository.findById(form.getAlumnoId())
                .orElseThrow(() -> NotFoundException.of("Alumno", form.getAlumnoId()));

        ActaDisciplinaria acta = new ActaDisciplinaria();
        acta.setAlumno(alumno);
        acta.setCicloLectivo(activo);
        acta.setTipo(form.getTipo());
        acta.setContenido(form.getContenido());
        acta.setGeneradaPor(generadaPor);
        acta = actaRepository.save(acta);

        String descripcion = "Se generó un Acta de %s para %s.".formatted(
                form.getTipo() == ActaDisciplinaria.Tipo.ACUERDO ? "Acuerdo" : "Matriculación", alumno.getNombreCompleto());
        notificarTutores(alumno, "Acta disciplinaria generada", descripcion);

        auditService.registrar(MODULO, "GENERAR_ACTA", "ActaDisciplinaria", acta.getId(), null, form.getTipo().name());
        return acta;
    }

    /** Alumnos que alcanzaron el umbral de 3 o 5 firmas sin tener aun el acta correspondiente (seccion 5.4.6). */
    @Transactional(readOnly = true)
    public List<AlumnoCicloLectivo> alumnosConActaPendiente() {
        CicloLectivo activo = cicloActivo();
        return alumnoCicloLectivoRepository.findAllByCicloLectivo_Id(activo.getId()).stream()
                .filter(acl -> acl.getFirmasAcumuladas() >= FIRMAS_ACTA_ACUERDO)
                .toList();
    }

    private void notificarTutores(Alumno alumno, String asunto, String descripcion) {
        notificacionService.notificarTutores(alumno, "DISCIPLINA", asunto, descripcion);
        List<AlumnoTutor> tutores = alumnoTutorRepository.findAllByAlumno_Id(alumno.getId());
        for (AlumnoTutor vinculo : tutores) {
            if (vinculo.isRecibeNotificaciones()) {
                emailService.enviarHtml(vinculo.getTutor().getEmail(), asunto, "<p>" + descripcion + "</p>");
            }
        }
    }

    private void notificarTutoresFirma(Alumno alumno, int numeroFirma) {
        notificacionService.notificarTutores(alumno, "FIRMA_DISCIPLINARIA", "Firma disciplinaria",
                "%s recibio la firma disciplinaria numero %d.".formatted(alumno.getNombreCompleto(), numeroFirma));
        List<AlumnoTutor> tutores = alumnoTutorRepository.findAllByAlumno_Id(alumno.getId());
        for (AlumnoTutor vinculo : tutores) {
            if (vinculo.isRecibeNotificaciones()) {
                emailService.enviarConPlantilla(vinculo.getTutor().getEmail(), "FIRMA_DISCIPLINARIA", Map.of(
                        "nombre_alumno", alumno.getNombreCompleto(),
                        "numero_firma", String.valueOf(numeroFirma)));
            }
        }
    }

    private CicloLectivo cicloActivo() {
        return cicloLectivoRepository.findByActivoTrue()
                .orElseThrow(() -> new BusinessException("No hay un ciclo lectivo activo configurado."));
    }
}
