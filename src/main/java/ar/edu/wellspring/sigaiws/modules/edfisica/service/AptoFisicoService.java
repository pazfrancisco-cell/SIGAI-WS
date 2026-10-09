package ar.edu.wellspring.sigaiws.modules.edfisica.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.notification.EmailService;
import ar.edu.wellspring.sigaiws.modules.mensajeria.service.NotificacionService;
import ar.edu.wellspring.sigaiws.common.storage.AlmacenamientoService;
import ar.edu.wellspring.sigaiws.modules.alerta.domain.Alerta;
import ar.edu.wellspring.sigaiws.modules.alerta.repository.AlertaRepository;
import ar.edu.wellspring.sigaiws.modules.alerta.service.AlertaService;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.Alumno;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoCicloLectivo;
import ar.edu.wellspring.sigaiws.modules.alumno.domain.AlumnoTutor;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoCicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.alumno.repository.AlumnoTutorRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.edfisica.domain.AptoFisico;
import ar.edu.wellspring.sigaiws.modules.edfisica.repository.AptoFisicoRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Seccion 5.7 (extension, no contemplada en la Propuesta Tecnica original):
 * apto fisico anual por alumno, vigente de abril a abril. A partir del 1 de
 * abril del año del ciclo lectivo activo, un alumno sin apto fisico presentado
 * queda "SAF" (Sin Apto Fisico): sigue yendo a la clase de Educacion Fisica,
 * pero no puede hacer actividad fisica (debe ayudar al docente o hacer un
 * trabajo practico en su lugar).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AptoFisicoService {

    private static final String MODULO = "EDUCACION_FISICA";
    private static final String TIPO_ALERTA = "APTO_FISICO_VENCIDO";
    private static final long DOCUMENTO_MAX_MB = 5;

    private final AptoFisicoRepository aptoFisicoRepository;
    private final CicloLectivoRepository cicloLectivoRepository;
    private final AlumnoCicloLectivoRepository alumnoCicloLectivoRepository;
    private final AlumnoTutorRepository alumnoTutorRepository;
    private final AlmacenamientoService almacenamientoService;
    private final AlertaService alertaService;
    private final AlertaRepository alertaRepository;
    private final EmailService emailService;
    private final NotificacionService notificacionService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Optional<AptoFisico> obtenerDelCicloActivo(Long alumnoId) {
        return cicloLectivoRepository.findByActivoTrue()
                .flatMap(ciclo -> aptoFisicoRepository.findByAlumno_IdAndCicloLectivo_Id(alumnoId, ciclo.getId()));
    }

    /** Seccion 5.7 (extension): a partir del 1 de abril del ciclo activo, sin apto fisico presentado = SAF. */
    @Transactional(readOnly = true)
    public boolean esSaf(Long alumnoId) {
        CicloLectivo activo = cicloLectivoRepository.findByActivoTrue().orElse(null);
        if (activo == null || LocalDate.now().isBefore(fechaLimite(activo))) {
            return false;
        }
        return !aptoFisicoRepository.existsByAlumno_IdAndCicloLectivo_Id(alumnoId, activo.getId());
    }

    public record ResumenAptoFisico(int presentados, int pendientes, LocalDate fechaLimite, boolean plazoVencido) {}

    @Transactional(readOnly = true)
    public ResumenAptoFisico resumenCicloActivo() {
        CicloLectivo activo = cicloLectivoRepository.findByActivoTrue()
                .orElseThrow(() -> new BusinessException("No hay un ciclo lectivo activo configurado."));
        var alumnosDelCiclo = alumnoCicloLectivoRepository.findAllByCicloLectivo_Id(activo.getId());
        long presentados = alumnosDelCiclo.stream()
                .filter(acl -> aptoFisicoRepository.existsByAlumno_IdAndCicloLectivo_Id(acl.getAlumno().getId(), activo.getId()))
                .count();
        LocalDate limite = fechaLimite(activo);
        return new ResumenAptoFisico((int) presentados, alumnosDelCiclo.size() - (int) presentados, limite, !LocalDate.now().isBefore(limite));
    }

    @Transactional
    public void presentar(Long alumnoId, MultipartFile documento, Usuario cargadoPor) {
        CicloLectivo activo = cicloLectivoRepository.findByActivoTrue()
                .orElseThrow(() -> new BusinessException("No hay un ciclo lectivo activo configurado."));
        AptoFisico apto = aptoFisicoRepository.findByAlumno_IdAndCicloLectivo_Id(alumnoId, activo.getId())
                .orElseGet(AptoFisico::new);
        Alumno alumnoRef = new Alumno();
        alumnoRef.setId(alumnoId);
        apto.setAlumno(alumnoRef);
        apto.setCicloLectivo(activo);
        apto.setFechaPresentacion(LocalDate.now());
        if (documento != null && !documento.isEmpty()) {
            var guardado = almacenamientoService.guardar(documento, "edfisica/apto-fisico", DOCUMENTO_MAX_MB);
            apto.setDocumentoPath(guardado.pathRelativo());
        }
        apto.setCargadoPor(cargadoPor);
        apto = aptoFisicoRepository.save(apto);
        auditService.registrar(MODULO, "PRESENTAR_APTO_FISICO", "AptoFisico", apto.getId(), null, "alumnoId=" + alumnoId);
    }

    /**
     * Corre todos los dias: a partir del 1 de abril del ciclo activo, genera una
     * unica alerta (para Directivo/Administrativo/Preceptor via Centro de
     * Alertas) y un email a los tutores por cada alumno que todavia no presento
     * el apto fisico de ese ciclo. Idempotente: no repite la alerta si ya se
     * genero una para ese alumno despues del inicio del ciclo activo.
     */
    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void verificarVencimientos() {
        CicloLectivo activo = cicloLectivoRepository.findByActivoTrue().orElse(null);
        if (activo == null || LocalDate.now().isBefore(fechaLimite(activo))) {
            return;
        }
        LocalDateTime generadaDesde = activo.getFechaInicioC1().atStartOfDay();

        for (AlumnoCicloLectivo acl : alumnoCicloLectivoRepository.findAllByCicloLectivo_Id(activo.getId())) {
            Alumno alumno = acl.getAlumno();
            if (aptoFisicoRepository.existsByAlumno_IdAndCicloLectivo_Id(alumno.getId(), activo.getId())) {
                continue;
            }
            if (alertaRepository.existsByTipoAndAlumno_IdAndGeneradaEnAfter(TIPO_ALERTA, alumno.getId(), generadaDesde)) {
                continue;
            }
            String descripcion = "%s no presento el apto fisico del ciclo %d. No puede hacer actividad fisica hasta presentarlo."
                    .formatted(alumno.getNombreCompleto(), activo.getAnio());
            alertaService.crear(TIPO_ALERTA, Alerta.Prioridad.ALTA, alumno, descripcion);
            notificarTutores(alumno, descripcion);
        }
    }

    private void notificarTutores(Alumno alumno, String descripcion) {
        notificacionService.notificarTutores(alumno, TIPO_ALERTA, "Apto fisico pendiente", descripcion);
        for (AlumnoTutor vinculo : alumnoTutorRepository.findAllByAlumno_Id(alumno.getId())) {
            if (vinculo.isRecibeNotificaciones()) {
                emailService.enviarHtml(vinculo.getTutor().getEmail(), "Apto fisico pendiente", "<p>" + descripcion + "</p>");
            }
        }
    }

    private LocalDate fechaLimite(CicloLectivo ciclo) {
        return LocalDate.of(ciclo.getAnio(), 4, 1);
    }
}
